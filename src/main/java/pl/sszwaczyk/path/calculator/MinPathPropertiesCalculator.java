package pl.sszwaczyk.path.calculator;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import pl.sszwaczyk.security.SecurityDimension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinPathPropertiesCalculator extends PathPropertiesCalculator {

    public MinPathPropertiesCalculator(IOFSwitchService switchService,
                                       ILinkDiscoveryService linkService) {
        super(switchService, linkService);
    }

    @Override
    public Map<SecurityDimension, Float> calculatePathProperties(List<IOFSwitch> switches, List<Link> links) {

        Map<SecurityDimension, Float> pathProperties = new HashMap<>();
        pathProperties.put(SecurityDimension.CONFIDENTIALITY, 1.0f);
        pathProperties.put(SecurityDimension.INTEGRITY, 1.0f);
        pathProperties.put(SecurityDimension.AVAILABILITY, 1.0f);
        pathProperties.put(SecurityDimension.TRUST, 1.0f);

        for(IOFSwitch s: switches) {
            Float trust = (Float) s.getAttributes().get(SecurityDimension.TRUST);
            if(trust < pathProperties.get(SecurityDimension.TRUST)) {
                pathProperties.put(SecurityDimension.TRUST, trust);
            }
        }

        for(Link l: links) {
            Float confidentiality = l.getConfidentiality();
            if(confidentiality < pathProperties.get(SecurityDimension.CONFIDENTIALITY)) {
                pathProperties.put(SecurityDimension.CONFIDENTIALITY, confidentiality);
            }

            Float integrity = l.getIntegrity();
            if(integrity < pathProperties.get(SecurityDimension.INTEGRITY)) {
                pathProperties.put(SecurityDimension.INTEGRITY, integrity);
            }

            Float availability = l.getAvailability();
            if(availability < pathProperties.get(SecurityDimension.AVAILABILITY)) {
                pathProperties.put(SecurityDimension.AVAILABILITY, availability);
            }
        }

        return pathProperties;
    }
}
