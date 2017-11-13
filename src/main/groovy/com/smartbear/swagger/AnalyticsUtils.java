package com.smartbear.swagger;

import com.smartbear.analytics.Analytics;
import com.smartbear.analytics.AnalyticsManager;
import org.apache.commons.collections.MapUtils;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsUtils {

    public static void sendAnalytics(String action, Map<String, String> actionParams) {
        HashMap params = new HashMap();
        params.put("SourceModule", "Projects");
        params.put("ProductArea", "NotSpecified");
        params.put("Type", "REST");
        params.put("Source", "Swagger");

        if (MapUtils.isNotEmpty(actionParams)) {
            for (Map.Entry entry : actionParams.entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            Analytics.getAnalyticsManager().trackAction(AnalyticsManager.Category.ACTION, action, params);
        } catch (Exception e) {

        }
    }
}
