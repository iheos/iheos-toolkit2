package gov.nist.toolkit.simcommon.server.factories

import gov.nist.toolkit.actortransaction.client.ActorType
import gov.nist.toolkit.configDatatypes.SimulatorProperties
import gov.nist.toolkit.configDatatypes.client.TransactionType
import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.client.Simulator
import gov.nist.toolkit.simcommon.client.SimulatorConfig
import gov.nist.toolkit.simcommon.server.AbstractActorFactory
import gov.nist.toolkit.simcommon.server.IActorFactory
import gov.nist.toolkit.simcommon.server.SimManager
import gov.nist.toolkit.sitemanagementui.client.Site
import gov.nist.toolkit.sitemanagementui.client.TransactionBean
import gov.nist.toolkit.xdsexception.NoSimulatorException

/**
 *
 */
class FhirActorFactory extends AbstractActorFactory implements IActorFactory {

    static final List<TransactionType> incomingTransactions =
            Arrays.asList(
                    TransactionType.FHIR
            );


    @Override
    protected Simulator buildNew(SimManager simm, SimId simId, boolean configureBase) throws Exception {
        ActorType actorType = ActorType.FHIR_SERVER
        SimulatorConfig sc
        if (configureBase)
            sc = configureBaseElements(actorType, simId)
        else
            sc = new SimulatorConfig()

        addFixedFhirEndpoint(sc, SimulatorProperties.fhirEndpoint, actorType, TransactionType.FHIR, false)
        addFixedFhirEndpoint(sc, SimulatorProperties.fhirTlsEndpoint, actorType, TransactionType.FHIR, true)


        return new Simulator(sc)
    }

    @Override
    protected void verifyActorConfigurationOptions(SimulatorConfig config) {

    }

    @Override
    Site getActorSite(SimulatorConfig asc, Site site) throws NoSimulatorException {
        String siteName = asc.getDefaultName()

        if (site == null)
            site = new Site(siteName)

        boolean isAsync = false

        site.addTransaction(new TransactionBean(
                TransactionType.FHIR.getCode(),
                TransactionBean.RepositoryType.NONE,
                asc.get(SimulatorProperties.fhirEndpoint).asString(),
                false,
                isAsync));
        site.addTransaction(new TransactionBean(
                TransactionType.FHIR.getCode(),
                TransactionBean.RepositoryType.NONE,
                asc.get(SimulatorProperties.fhirTlsEndpoint).asString(),
                true,
                isAsync));

        return site
    }

    @Override
    List<TransactionType> getIncomingTransactions() {
        return incomingTransactions
    }

    @Override
    ActorType getActorType() {
        return ActorType.FHIR_SERVER
    }
}