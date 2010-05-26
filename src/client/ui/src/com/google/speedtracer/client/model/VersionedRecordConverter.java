/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.client.model;

import com.google.gwt.coreext.client.IterableFastStringMap;

/**
 * Converts Speed Tracer records from a certain version to the current version.
 */
public abstract class VersionedRecordConverter {
  /**
   * Converts records from version 0.8 to their counterparts in the current
   * version.
   * 
   * NOTE: If the record format changes again, the patch should also update this
   * converter, potentially chaining the current implementation to the resulting
   * 0.9->current implementation that would need to be created.
   */
  private static class RecordConverter_0_8 extends VersionedRecordConverter {
    /**
     * Overlay for the old network resource start event.
     */
    static class OldNetworkResource extends EventRecord {
      protected OldNetworkResource() {
      }

      final String getResourceId() {
        return getData().getStringProperty("resourceId");
      }
    }

    static class OldResponse extends OldNetworkResource {
      @SuppressWarnings("all")
      protected OldResponse() {
      }

      final String getMimeType() {
        return getData().getStringProperty("mimeType");
      }

      final int getResponseCode() {
        return getData().getIntProperty("responseCode");
      }
    }

    static class OldStart extends OldNetworkResource {
      @SuppressWarnings("all")
      protected OldStart() {
      }

      final String getHttpMethod() {
        return getData().getStringProperty("httpMethod");
      }

      final String getUrl() {
        return getData().getStringProperty("url");
      }
    }
    /**
     * Utility class for provisioning IDs for network resources.
     */
    static class ResourceIDIterator {
      // We changed from string IDs to int IDs. So we can make them up now.
      static int resourceCounter = 0;
      IterableFastStringMap<Integer> idMap = new IterableFastStringMap<Integer>();

      int getNextId(String oldId) {
        Integer id = idMap.get(oldId);
        if (id != null) {
          return id;
        } else {
          resourceCounter++;
          idMap.put(oldId, new Integer(resourceCounter));
          return resourceCounter;
        }
      }
    }

    static final int OLD_ERROR_TYPE = 21;

    static final int OLD_FINISH_TYPE = 22;

    static final int OLD_RESPONSE_TYPE = 23;

    static final int OLD_START_TYPE = 24;

    static final int OLD_TAB_CHANGE_TYPE = 16;

    static native ResourceFinishEvent createFinishEvent(int id, double time,
        boolean didFail) /*-{
      return {
        type: @com.google.speedtracer.client.model.ResourceFinishEvent::TYPE,
        time: time,
        data: {
          identifier: id,
          didFail: didFail
        }
      };
    }-*/;

    static native ResourceResponseEvent createResponseEvent(int id,
        double time, String mimeType, int responseCode) /*-{
      return {
        type: @com.google.speedtracer.client.model.ResourceResponseEvent::TYPE,
        time: time,
        data: {
          identifier: id,
          mimeType: mimeType,
          statusCode: responseCode,
          expectedContentLength: 0
        }
      };
    }-*/;

    static native ResourceWillSendEvent createWillSendEvent(int id,
        double time, String method, String url, boolean isMainResource) /*-{
      return {
        type: @com.google.speedtracer.client.model.ResourceWillSendEvent::TYPE,
        time: time,
        data: {
          identifier: id,
          requestMethod: method,
          url: url,
          isMainResource: isMainResource
        }
      }
    }-*/;

    ResourceIDIterator idIter = new ResourceIDIterator();

    IterableFastStringMap<OldStart> networkStartIdMap = new IterableFastStringMap<OldStart>();

    IterableFastStringMap<OldStart> networkStartUrlMap = new IterableFastStringMap<OldStart>();

    /**
     * What changed in this version:
     * 
     * We split apart the record enums so that Speed Tracer customr records and
     * WebKit timeline records didnt share the same enum space.
     * 
     * TabChanged events are treated differently. We no longer synthesize our
     * own network resource events.
     */
    @Override
    public void convert(DataInstance dataInstance, EventRecord record) {
      int type = record.getType();
      switch (type) {
        case OLD_TAB_CHANGE_TYPE:
          // In addition to changing the record type, we
          // actually changed the way we perform page transitions. We used to
          // fire page transitions right after a start event.
          handlePageTransition(dataInstance, record);
          break;
        case OLD_START_TYPE:
          // Network resource start.
          // Keep them around for just a bit In case we want to send a page
          // transition.
          OldStart mainResourceStart = record.cast();
          networkStartUrlMap.put(mainResourceStart.getUrl(), mainResourceStart);
          networkStartIdMap.put(mainResourceStart.getResourceId(),
              mainResourceStart);
          break;
        case OLD_RESPONSE_TYPE:
          handleNetworkResponse(dataInstance, record);
          break;
        case OLD_FINISH_TYPE:
          // Network resource finish.
          // Simply send it.
          sendFinish(dataInstance, record, false);
          break;
        case OLD_ERROR_TYPE:
          // Network resource error.
          // Simply send it as a finish.
          sendFinish(dataInstance, record, true);
          break;
        default:
          dataInstance.onEventRecord(record);
          break;
      }
    }

    private void handleNetworkResponse(DataInstance dataInstance,
        EventRecord record) {
      OldResponse response = record.cast();
      OldStart resourceStart = networkStartIdMap.get(response.getResourceId()).cast();
      if (resourceStart != null) {
        sendStart(resourceStart, dataInstance);
      }

      // Create and send the response.
      dataInstance.onEventRecord(createResponseEvent(
          idIter.getNextId(response.getResourceId()), response.getTime(),
          response.getMimeType(), response.getResponseCode()));
    }

    private void handlePageTransition(DataInstance dataInstance,
        EventRecord record) {
      PageTransition pageTransition = record.cast();
      OldStart mainResourceStart = networkStartUrlMap.get(
          pageTransition.getUrl()).cast();
      if (mainResourceStart != null) {
        // Send the page transition.
        dataInstance.onEventRecord(PageTransition.create(
            mainResourceStart.getTime(), pageTransition.getUrl()));

        sendStart(mainResourceStart, dataInstance);
      }
    }

    private void sendFinish(DataInstance dataInstance, EventRecord record,
        boolean didFail) {
      OldNetworkResource finish = record.cast();
      dataInstance.onEventRecord(createFinishEvent(
          idIter.getNextId(finish.getResourceId()), finish.getTime(), didFail));
    }

    private void sendStart(OldStart oldStart, DataInstance dataInstance) {
      // Now send the start.
      dataInstance.onEventRecord(createWillSendEvent(
          idIter.getNextId(oldStart.getResourceId()), oldStart.getTime(),
          oldStart.getHttpMethod(), oldStart.getUrl(), true));

      // Remove it.
      networkStartUrlMap.remove(oldStart.getUrl());
      networkStartIdMap.remove(oldStart.getResourceId());
    }
  }

  /**
   * For unknown version strings, we return a null impl.
   */
  private static class UnknownRecordConverter extends VersionedRecordConverter {
    @Override
    public void convert(DataInstance dataInstance, EventRecord record) {
      // It is a versioned file that is not current, but does not have a known
      // record converter. We assume then that the version change does not
      // contain a change in the record format.
      dataInstance.onEventRecord(record);
    }
  }

  public static final String VERSION_0_8 = "0.8";

  /**
   * Static factory method for obtaining a VersionedRecordConverter.
   * 
   * @param version the version String for the types of records we want to
   *          convert from.
   * @return a VersionedRecordConverter that converts to the current style.
   */
  public static VersionedRecordConverter create(String version) {
    if (VERSION_0_8.equals(version)) {
      return new RecordConverter_0_8();
    }

    // TODO (jaimeyap): We dont want to throw an exception since we might be
    // dealing with loading a corrupted file or something.
    return new UnknownRecordConverter();
  }

  private VersionedRecordConverter() {
  }

  /**
   * Converts an inputed record to the current version of that record, and send
   * it on to the {@link DataInstance} that consumes it.
   * 
   * @param dataInstance the recipient of the converted message.
   * @param record the old style record we want to convert to a current style
   *          one.
   */
  public abstract void convert(DataInstance dataInstance, EventRecord record);
}
