/*
 * Copyright 2009 Google Inc.
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
package com.google.speedtracer.client.visualizations.view;

import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;
import com.google.speedtracer.client.visualizations.view.HintletReportTree.ReportDetails;

import java.util.List;

/**
 * Implements a table showing a list of hintlet records that is sortable.
 * 
 */
public class HintletReportRowDetails extends ReportDetails {

  public HintletReportRowDetails(Tree.Item parent,
      List<HintRecord> hintletRecords, HintletReport.Resources resources,
      HintletReportModel reportModel) {
    super(parent, hintletRecords, resources, reportModel);

    addColumn(HintletReportTree.COL_SEVERITY);
    addColumn(HintletReportTree.COL_TIME);
    addColumn(HintletReportTree.COL_RULE_NAME);
    addColumn(HintletReportTree.COL_DESCRIPTION);

    // The data rows are populated by the sort handler firing.
    setSortColumn(HintletReportTree.COL_TIME, false);
  }

}
