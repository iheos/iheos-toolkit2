package gov.nist.toolkit.simcommon.server;

import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.Simulator;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.sitemanagementui.client.Site;

import java.util.List;

/**
 * This class exists only to expose methods in ActorFactory such as
 * the buildNewSimulator method which
 * would otherwise be buried in the abstract class ActorFactory.
 * @author bill
 *
 */
public class GenericSimulatorFactory extends AbstractActorFactory {
	SimId newID = null;
	@SuppressWarnings("unused")
	public GenericSimulatorFactory() {
	}
	
	public GenericSimulatorFactory(SimManager simManager) {
		super();
	}

	// ActorFactory needs to be directly
	public Simulator buildNewSimulator(SimManager simm, String simtype, SimId simID) throws Exception {
		return buildNewSimulator(simm, simtype, simID, true);
	}

	@Override
	protected Simulator buildNew(SimManager simm, SimId newID, boolean configureBase) throws Exception {
		this.newID = newID;
		return null;
	}


	@Override
	protected void verifyActorConfigurationOptions(SimulatorConfig config) {

	}

	@Override
	public Site getActorSite(SimulatorConfig asc, Site site) {
		return null;
	}

	@Override
	public List<TransactionType> getIncomingTransactions() {
		return null;
	}
	

}