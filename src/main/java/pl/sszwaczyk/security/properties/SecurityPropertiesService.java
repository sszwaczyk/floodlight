package pl.sszwaczyk.security.properties;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.floodlightcontroller.restserver.IRestApiService;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.sszwaczyk.security.SecurityDimension;
import pl.sszwaczyk.security.properties.web.LinkSecurityProperties;
import pl.sszwaczyk.security.properties.web.SecurityPropertiesWebRoutable;
import pl.sszwaczyk.security.properties.web.SwitchSecurityProperties;
import pl.sszwaczyk.security.soc.*;

import java.util.*;

public class SecurityPropertiesService implements IFloodlightModule, IOFSwitchListener, ILinkDiscoveryListener,
        ISOCListener, ISecurityPropertiesService {

    private static final Logger log = LoggerFactory.getLogger(SecurityPropertiesService.class);

    private IRestApiService restApiService;
    private IOFSwitchService switchService;
    private ILinkDiscoveryService linkService;
    private ISOCService socService;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> s =
                new HashSet<Class<? extends IFloodlightService>>();
        s.add(ISecurityPropertiesService.class);
        return s;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        m.put(ISecurityPropertiesService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IRestApiService.class);
        l.add(IOFSwitchService.class);
        l.add(ILinkDiscoveryService.class);
        l.add(ISOCService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        this.restApiService = context.getServiceImpl(IRestApiService.class);
        this.switchService = context.getServiceImpl(IOFSwitchService.class);
        this.linkService = context.getServiceImpl(ILinkDiscoveryService.class);
        this.socService = context.getServiceImpl(ISOCService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        restApiService.addRestletRoutable(new SecurityPropertiesWebRoutable());
        switchService.addOFSwitchListener(this);
        linkService.addListener(this);
        socService.addListener(this);
    }

    @Override
    public void switchAdded(DatapathId switchId) {
        log.debug("Switch added handling ({})", switchId);
        IOFSwitch newSwitch = switchService.getSwitch(switchId);
        newSwitch.getAttributes().put(SecurityDimension.TRUST, 0.99f);
        log.info("Trust for new switch {} set to 0.99", switchId);
    }

    @Override
    public void switchRemoved(DatapathId switchId) {
    }

    @Override
    public void switchActivated(DatapathId switchId) {
    }

    @Override
    public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
    }

    @Override
    public void switchChanged(DatapathId switchId) {
    }

    @Override
    public void switchDeactivated(DatapathId switchId) {
    }

    @Override
    public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
        for(LDUpdate ldUpdate: updateList) {
            if(ldUpdate.getOperation().equals(UpdateOperation.LINK_UPDATED)) {
                for(Link link: linkService.getLinks().keySet()) {
                    if(link.getSrc().equals(ldUpdate.getSrc())
                        && link.getDst().equals(ldUpdate.getDst())
                        && link.getSrcPort().equals(ldUpdate.getSrcPort())
                        && link.getDstPort().equals(ldUpdate.getDstPort())
                        && link.getSecurityProperties() == null) {
                            log.debug("Handling link update ({})", link);
                            Map<SecurityDimension, Float> securityProperties = new HashMap<>();
                            securityProperties.put(SecurityDimension.CONFIDENTIALITY, 0.99f);
                            securityProperties.put(SecurityDimension.INTEGRITY, 0.99f);
                            securityProperties.put(SecurityDimension.AVAILABILITY, 0.99f);
                            link.setSecurityProperties(securityProperties);
                            log.info("C, I, A for new link {} set to 0.99", link);
                    }
                }
            }
        }
    }

    @Override
    public void socUpdate(SOCUpdate socUpdate) {
        log.info("SOC update {} received", socUpdate);

        SOCUpdateType type = socUpdate.getType();
        Map<SecurityDimension, Float> securityPropertiesDifference = socUpdate.getSecurityPropertiesDifference();

        if(type.equals(SOCUpdateType.THREAT_ACTIVATED_SWITCH)) {

            IOFSwitch s = switchService.getSwitch(socUpdate.getSrc());
            Float actualTrust = (Float) s.getAttributes().get(SecurityDimension.TRUST);
            Float trustDifference = securityPropertiesDifference.get(SecurityDimension.TRUST);
            if(trustDifference > actualTrust) {
                log.warn("New threat TRUST difference more than actual TRUST for switch {}. Setting TRUST to 0.", socUpdate.getSrc());
                s.getAttributes().put(SecurityDimension.TRUST, 0.0f);
            } else {
                s.getAttributes().put(SecurityDimension.TRUST, actualTrust - trustDifference);
                log.info("Set TRUST for switch {} to {}", socUpdate.getSrc(), s.getAttributes().get(SecurityDimension.TRUST));
            }

        } else if(type.equals(SOCUpdateType.THREAT_ACTIVATED_LINK)) {

            for(Link link: linkService.getLinks().keySet()) {
                if(link.getSrc().equals(socUpdate.getSrc())
                        && link.getDst().equals(socUpdate.getDst())
                        && link.getSrcPort().equals(socUpdate.getSrcPort())
                        && link.getDstPort().equals(socUpdate.getDstPort())) {

                    Float actualConfidentiality = link.getConfidentiality();
                    Float confidentialityDifference = securityPropertiesDifference.get(SecurityDimension.CONFIDENTIALITY);
                    if(confidentialityDifference > actualConfidentiality) {
                        log.warn("New threat CONFIDENTIALITY difference more than actual CONFIDENTIALITY for link {}. Setting CONFIDENTIALITY to 0.", link);
                        link.setConfidentiality(0.0f);
                    } else {
                        link.setConfidentiality(actualConfidentiality - confidentialityDifference);
                    }

                    Float actualIntegrity = link.getIntegrity();
                    Float integrityDifference = securityPropertiesDifference.get(SecurityDimension.INTEGRITY);
                    if(integrityDifference > actualIntegrity) {
                        log.warn("New threat INTEGRITY difference more than actual INTEGRITY for link {}. Setting INTEGRITY to 0.", link);
                        link.setIntegrity(0.0f);
                    } else {
                        link.setIntegrity(actualIntegrity - integrityDifference);
                    }

                    Float actualAvailability = link.getAvailability();
                    Float availabilityDifference = securityPropertiesDifference.get(SecurityDimension.AVAILABILITY);
                    if(availabilityDifference > actualAvailability) {
                        log.warn("New threat AVAILABILITY difference more than actual AVAILABILITY for link {}. Setting AVAILABILITY to 0.", link);
                        link.setAvailability(0.0f);
                    } else {
                        link.setAvailability(actualAvailability - availabilityDifference);
                    }

                    log.info("Set new C, I, A for link {}", link);
                    log.info("Confidentiality = {}", link.getConfidentiality());
                    log.info("Integrity = {}", link.getIntegrity());
                    log.info("Availability = {}", link.getAvailability());

                    break;
                }
            }

        } else if(type.equals(SOCUpdateType.THREAT_ENDED_SWITCH)) {

            IOFSwitch s = switchService.getSwitch(socUpdate.getSrc());
            Float actualTrust = (Float) s.getAttributes().get(SecurityDimension.TRUST);
            Float trustDifference = securityPropertiesDifference.get(SecurityDimension.TRUST);
            if(actualTrust + trustDifference > 0.99) {
                log.warn("Threat ended TRUST plus actual TRUST is more than 0.99 for switch {}. Setting TRUST to 0.99", socUpdate.getSrc());
                s.getAttributes().put(SecurityDimension.TRUST, 0.99f);
            } else {
                s.getAttributes().put(SecurityDimension.TRUST, actualTrust + trustDifference);
                log.info("Set TRUST for switch {} to {}", socUpdate.getSrc(), s.getAttributes().get(SecurityDimension.TRUST));
            }

        } else if(type.equals(SOCUpdateType.THREAT_ENDED_LINK)) {

            for(Link link: linkService.getLinks().keySet()) {
                if(link.getSrc().equals(socUpdate.getSrc())
                        && link.getDst().equals(socUpdate.getDst())
                        && link.getSrcPort().equals(socUpdate.getSrcPort())
                        && link.getDstPort().equals(socUpdate.getDstPort())) {

                    Float actualConfidentiality = link.getConfidentiality();
                    Float confidentialityDifference = securityPropertiesDifference.get(SecurityDimension.CONFIDENTIALITY);
                    if(actualConfidentiality + confidentialityDifference > 0.99) {
                        log.warn("Threat ended CONFIDENTIALITY plus actual CONFIDENTIALITY for link {} is more than 0.99. Setting CONFIDENTIALITY to 0.99.", link);
                        link.setConfidentiality(0.99f);
                    } else {
                        link.setConfidentiality(actualConfidentiality + confidentialityDifference);
                    }

                    Float actualIntegrity = link.getIntegrity();
                    Float integrityDifference = securityPropertiesDifference.get(SecurityDimension.INTEGRITY);
                    if(actualIntegrity + integrityDifference > 0.99) {
                        log.warn("Threat ended INTEGRITY plus actual INTEGRITY for link {} is more than 0.99. Setting INTEGRITY to 0.99.", link);
                        link.setIntegrity(0.99f);
                    } else {
                        link.setIntegrity(actualIntegrity + integrityDifference);
                    }

                    Float actualAvailability = link.getAvailability();
                    Float availabilityDifference = securityPropertiesDifference.get(SecurityDimension.AVAILABILITY);
                    if(actualAvailability + availabilityDifference > 0.99) {
                        log.warn("Threat ended AVAILABILITY plus actual AVAILABILITY for link {} is more than 0.99. Setting AVAILABILITY to 0.99.", link);
                        link.setAvailability(0.99f);
                    } else {
                        link.setAvailability(actualAvailability + availabilityDifference);
                    }

                    log.info("Set new C, I, A for link {}", link);
                    log.info("Confidentiality = {}", link.getConfidentiality());
                    log.info("Integrity = {}", link.getIntegrity());
                    log.info("Availability = {}", link.getAvailability());

                    break;
                }
            }

        }

    }

    @Override
    public List<SwitchSecurityProperties> getSwitchesSecurityProperties() {
        List<SwitchSecurityProperties> properties = new ArrayList<>();

        Map<DatapathId, IOFSwitch> allSwitches = switchService.getAllSwitchMap();
        allSwitches.values().forEach(s -> {
            SwitchSecurityProperties props = new SwitchSecurityProperties();
            props.setSwitchDpid(s.getId().toString());
            props.setTrust((Float) s.getAttributes().get(SecurityDimension.TRUST));
            properties.add(props);
        });

        return properties;
    }

    @Override
    public List<LinkSecurityProperties> getLinksSecurityProperties() {
        List<LinkSecurityProperties> properties = new ArrayList<>();

        Map<Link, LinkInfo> allLinks = linkService.getLinks();
        allLinks.keySet().forEach(l -> {
            LinkSecurityProperties props = new LinkSecurityProperties();
            props.setSrc(l.getSrc().toString());
            props.setSrcPort(l.getSrcPort().getPortNumber());
            props.setDst(l.getDst().toString());
            props.setDstPort(l.getDstPort().getPortNumber());
            props.setConfidentiality(l.getConfidentiality());
            props.setIntegrity(l.getIntegrity());
            props.setAvailability(l.getAvailability());
            properties.add(props);
        });

        return properties;
    }

    @Override
    public void setLinkSecurityProperites(LinkSecurityProperties properties) {
        Map<Link, LinkInfo> links = linkService.getLinks();
        for(Link link: linkService.getLinks().keySet()) {
            if(link.getSrc().equals(DatapathId.of(properties.getSrc()))
                    && link.getDst().equals(DatapathId.of(properties.getDst()))
                    && link.getSrcPort().equals(OFPort.of(properties.getSrcPort()))
                    && link.getDstPort().equals(OFPort.of(properties.getDstPort()))) {
                link.setConfidentiality(properties.getConfidentiality());
                link.setIntegrity(properties.getIntegrity());
                link.setAvailability(properties.getAvailability());
                log.info("Set new C, I, A for link {}", link);
                log.info("Confidentiality = {}", link.getConfidentiality());
                log.info("Integrity = {}", link.getIntegrity());
                log.info("Availability = {}", link.getAvailability());
            }
        }
    }
}
