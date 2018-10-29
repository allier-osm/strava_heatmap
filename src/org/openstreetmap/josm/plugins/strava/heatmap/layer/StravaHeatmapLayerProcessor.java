package org.openstreetmap.josm.plugins.strava.heatmap.layer;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.strava.heatmap.authentication.StravaCookiesRetriever;
import org.openstreetmap.josm.plugins.strava.heatmap.authentication.StravaAuthenticationException;
import org.openstreetmap.josm.plugins.strava.heatmap.http.StravaHttpException;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Processor that adds the Strava authentication cookies to the Strava heatmap layers.
 */
public class StravaHeatmapLayerProcessor {


    /**
     * The list of default Strava layers published in the JOSM imagery preferences.
     */
    private static final List<String> STRAVA_LAYER_IDS = Arrays.asList(new String[]{"strava_cycling_heatmap", "strava_running_heatmap", "strava_both_heatmap", "strava_water_heatmap", "strava_winter_heatmap"});


    /**
     * If the layer is a Strava layer, then adds the authentication cookies.
     *
     * @param layer the layer being opened.
     */
    public void processLayer(Layer layer) {
        if (isStravaHeatmapLayer(layer)) {
            updateImageryInfo(((TMSLayer) layer).getInfo());
        }
    }

    /**
     * Tests if the layer is one of the default Strava layers.
     *
     * @param layer the layer being opened.
     * @return true if the layer is a default Strava layer.
     */
    private boolean isStravaHeatmapLayer(Layer layer) {
        if (layer instanceof TMSLayer) {
            TMSLayer tmsLayer = (TMSLayer) layer;
            ImageryInfo imageryInfo = tmsLayer.getInfo();
            if (imageryInfo != null) {
                String layerId = imageryInfo.getId();
                if (STRAVA_LAYER_IDS.contains(layerId)) {
                    if (Logging.isDebugEnabled()) {
                        Logging.debug("The current layer is a Strava layer " + layerId);
                    }
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Update the URL and add the authentication cookies to the default imagery information.
     * TODO use {@link StravaCookiesRetriever#getCookiesAsHttpHeader()} when upgrading to JOSM > 14382
     *
     * @param imageryInfo the layer's imagery info.
     */
    private void updateImageryInfo(ImageryInfo imageryInfo) {

        // get cookies as request parameters (ImageryInfo does not expose a method to set cookies as HTTP header)
        StravaCookiesRetriever cookiesRetriever = new StravaCookiesRetriever();
        try {
            String cookies = cookiesRetriever.getCookiesAsRequestParameters();
            // switch to authenticated tile server and append cookies as request parameters
            String oldUrl = imageryInfo.getUrl();
            String newUrl = oldUrl.replace("/tiles/", "/tiles-auth/").concat(cookies);
            imageryInfo.setUrl(newUrl);

            imageryInfo.setDefaultMaxZoom(15);
        } catch (StravaAuthenticationException e) {
            Logging.error(e);
            GuiHelper.runInEDT(() ->
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            I18n.tr("Authentication on the Strava website failed.\n"
                                    + "Please check your Strava account details in the plugin preferences page.\n"
                                    + "Without authentication only the low-resolution heatmap will be available.")));

        } catch (StravaHttpException e) {
            Logging.error(e);
            GuiHelper.runInEDT(() ->
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            I18n.tr("An error occurred when trying to authenticate on the Strava website.\n"
                                    + "This may be a temporary network issue.\n"
                                    + "Without authentication only the low-resolution heatmap will be available.")));
        }
    }
}
