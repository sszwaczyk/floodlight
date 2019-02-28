package pl.sszwaczyk.repository.web;

import net.floodlightcontroller.routing.Path;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import pl.sszwaczyk.repository.ISecureFlowsRepository;
import pl.sszwaczyk.utils.AddressesAndPorts;

import java.util.Map;

public class SecureFlowsRepositoryResource extends ServerResource {

    @Get("json")
    public Map<AddressesAndPorts, Path> getActualSecurityFlows() {
        ISecureFlowsRepository secureFlowsRepository =
                (ISecureFlowsRepository) getContext().getAttributes().
                        get(ISecureFlowsRepository.class.getCanonicalName());

        return secureFlowsRepository.getFlows();
    }

}
