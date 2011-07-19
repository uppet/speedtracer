/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.speedtracer.client.model.DataDispatcher.DataDispatcherDelegate;
import com.google.speedtracer.client.model.DataDispatcher.EventRecordDispatcher;
import com.google.speedtracer.client.model.NetworkResponseReceivedEvent.Response;
import com.google.speedtracer.client.model.ResourceUpdateEvent.UpdateResource;
import com.google.speedtracer.shared.EventRecordType;

import java.util.ArrayList;
import java.util.List;

/**
 * Native dispatcher which sources Network Events for the UI. Hooks into
 * underlying DataInstance.
 */
public class NetworkEventDispatcher implements DataDispatcherDelegate {

  /**
   * Listener Interface for a NetworkEventDispatcher.
   */
  public interface Listener {
    void onNetworkResourceRequestStarted(NetworkResource resource, boolean isRedirect);

    void onNetworkResourceResponseFinished(NetworkResource resource);

    void onNetworkResourceResponseStarted(NetworkResource resource);

    void onNetworkResourceUpdated(NetworkResource resource);
  }

  /**
   * Sets up mapping of NetworkResourceRecord types to their respective
   * handlers.
   *
   * @param proxy the {@link NetworkEventDispatcher}
   * @param typeMap the {@link FastStringMap}
   */
  private static void setNetworkEventCallbacks(
      final NetworkEventDispatcher proxy, JsIntegerMap<EventRecordDispatcher> typeMap) {

    typeMap.put(EventRecordType.RESOURCE_SEND_REQUEST, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceStarted(data.<ResourceWillSendEvent>cast());
      }
    });

    typeMap.put(EventRecordType.RESOURCE_RECEIVE_RESPONSE, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceResponse(data.<ResourceResponseEvent>cast());
      }
    });

    typeMap.put(EventRecordType.RESOURCE_FINISH, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        ResourceFinishEvent finish = data.cast();
        proxy.onNetworkResourceFinished(finish);
      }
    });

    typeMap.put(EventRecordType.RESOURCE_UPDATED, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceUpdated(data.<ResourceUpdateEvent>cast());
      }
    });

    typeMap.put(EventRecordType.NETWORK_DATA_RECEIVED, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkDataReceived(data.<NetworkDataReceivedEvent>cast());
      }
    });

    typeMap.put(EventRecordType.NETWORK_RESPONSE_RECEIVED, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResponseReceived(data.<NetworkResponseReceivedEvent>cast());
      }
    });

    typeMap.put(EventRecordType.NETWORK_REQUEST_WILL_BE_SENT, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkRequestWillBeSent(data.<NetworkRequestWillBeSentEvent>cast());
      }
    });
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  private final List<ResourceRecord> networkEvents = new ArrayList<ResourceRecord>();

  private JsIntegerMap<JSOArray<NetworkResource>> redirects = JsIntegerMap.create();

  /**
   * Map of NetworkResource POJOs. Information about a network resource is
   * filled in progressively as we get chunks of information about it.
   */
  private final JsIntegerMap<NetworkResource> resourceStore = JsIntegerMap.create();

  private final JsIntegerMap<EventRecordDispatcher> typeMap = JsIntegerMap.create();

  public NetworkEventDispatcher() {
    setNetworkEventCallbacks(this, typeMap);
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void clearData() {
    redirects = JsIntegerMap.create();
    networkEvents.clear();
  }

  public List<ResourceRecord> getNetworkEvents() {
    return networkEvents;
  }

  /**
   * Gets a stored resource from our book keeping, or null if it hasnt been
   * stored before.
   *
   * @param id the request id of our {@link NetworkResource}
   * @return returns the {@link NetworkResource}
   */
  public NetworkResource getResource(int id) {
    return resourceStore.get(id);
  }

  public void onEventRecord(EventRecord data) {
    final EventRecordDispatcher handler = typeMap.get(data.getType());
    if (handler != null) {
      handler.onEventRecord(data);
    }
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  /**
   * @param identifier Resource ID
   * @param url The redirect URL that we are using to match a redirect
   *        candidate.
   */
  private NetworkResource findAndRemoveRedirectCandidate(int identifier, String url) {
    // Look for it.
    JSOArray<NetworkResource> redirectCandidates = redirects.get(identifier);
    if (redirectCandidates == null) {
      return null;
    }

    for (int i = 0; i < redirectCandidates.size(); i++) {
      NetworkResource redirectCandidate = redirectCandidates.get(i);
      if (redirectCandidate.getUrl().equals(url)) {
        // Should not be a concurrent modification, since we now bail out of
        // the loop right after mutating the queue.
        redirectCandidates.splice(i, 1);
        return redirectCandidate;
      }
    }

    return null;
  }

  private void insertRedirectCandidate(int identifier, NetworkResource previousResource) {
    // We have a redirect.
    JSOArray<NetworkResource> redirectQueue = redirects.get(identifier);
    if (redirectQueue == null) {
      redirectQueue = JSOArray.create().cast();
      redirects.put(identifier, redirectQueue);
    }
    redirectQueue.push(previousResource);
  }

  private void onNetworkDataReceived(
      NetworkDataReceivedEvent dataLengthChange) {
    NetworkResource resource = getResource(dataLengthChange.getIdentifier());
    if (resource != null) {
      resource.update(dataLengthChange);
    }
  }

  private void onNetworkResponseReceived(NetworkResponseReceivedEvent response) {
    NetworkResource resource = getResource(response.getIdentifier());
    if (resource != null) {
      resource.update(response);
    }
  }

  private void onNetworkRequestWillBeSent(NetworkRequestWillBeSentEvent requestWillBeSent) {
    NetworkResource resource = getResource(requestWillBeSent.getIdentifier());
    if (resource != null) {
      resource.update(requestWillBeSent);
      // We depend on the fact that any redirects that we encounter will have
      // a corresponding timeline agent event that will add it to the redirects
      // map. We look for one here.
      NetworkRequestWillBeSentEvent.Data data = requestWillBeSent.getData().cast();
      Response redirectResponse = data.getRedirectResponse();
      if (redirectResponse != null) {
        // look for a redirect.
        NetworkResource redirect = findAndRemoveRedirectCandidate(
            requestWillBeSent.getIdentifier(), redirectResponse.getUrl());
        if (redirect != null) {
          redirect.updateResponse(redirectResponse);
          redirect.setResponseReceivedTime(requestWillBeSent.getTime());
          redirect.setEndTime(requestWillBeSent.getTime());
          redirectUpdated(redirect);
        }
      }
    }
  }

  private void onNetworkResourceFinished(ResourceFinishEvent resourceFinish) {
    networkEvents.add(resourceFinish);
    NetworkResource resource = getResource(resourceFinish.getIdentifier());
    if (resource != null) {
      resource.update(resourceFinish);
      for (int i = 0, n = listeners.size(); i < n; i++) {
        Listener listener = listeners.get(i);
        listener.onNetworkResourceResponseFinished(resource);
      }
    }
  }

  private void onNetworkResourceResponse(ResourceResponseEvent resourceResponse) {
    networkEvents.add(resourceResponse);
    NetworkResource resource = getResource(resourceResponse.getIdentifier());
    if (resource != null) {
      resource.update(resourceResponse);
      for (int i = 0, n = listeners.size(); i < n; i++) {
        Listener listener = listeners.get(i);
        listener.onNetworkResourceResponseStarted(resource);
      }
    }
  }

  private void onNetworkResourceStarted(ResourceWillSendEvent resourceStart) {
    networkEvents.add(resourceStart);
    // Check for dupe IDs. If we find one, assume it is a redirect.
    NetworkResource previousResource = getResource(resourceStart.getIdentifier());
    boolean isRedirect = false;
    if (previousResource != null) {
      insertRedirectCandidate(previousResource.getIdentifier(), previousResource);
      isRedirect = true;
    }
    
    NetworkResource resource = new NetworkResource(resourceStart);
    resourceStore.put(resourceStart.getIdentifier(), resource);
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceRequestStarted(resource, isRedirect);
    }
  }

  /**
   * This should now be dead code in the live tracing case, and is only kept to
   * support loading old saved dumps.
   */
  private void onNetworkResourceUpdated(ResourceUpdateEvent update) {
    NetworkResource resource = getResource(update.getIdentifier());
    if (resource != null) {
      resource.update(update);
      for (int i = 0, n = listeners.size(); i < n; i++) {
        Listener listener = listeners.get(i);
        listener.onNetworkResourceUpdated(resource);
      }
    } else {
      // We are dealing potentially with an update for a redirect.
      UpdateResource updateResource = update.getUpdate();
      NetworkResource redirectCandidate =
          findAndRemoveRedirectCandidate(update.getIdentifier(), updateResource.getUrl());
      if (redirectCandidate != null) {
        redirectCandidate.update(update);
        redirectUpdated(redirectCandidate);
      }
    }
  }

  private void redirectUpdated(NetworkResource redirectCandidate) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceUpdated(redirectCandidate);
    }
  }
}
