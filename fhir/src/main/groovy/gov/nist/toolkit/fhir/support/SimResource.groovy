package gov.nist.toolkit.fhir.support

import gov.nist.toolkit.actortransaction.shared.ActorType
import gov.nist.toolkit.configDatatypes.client.TransactionType
import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.server.SimDb

import java.util.regex.Pattern

/**
 * This contains details about where the index is stored and
 * what actor/transaction/event it belongs to.
 */
class SimResource {
    String actor
    String transaction
    String event   // name not path
    String filename  // name not path

    SimResource(SimDb simDb) {
        actor = simDb.actor
        transaction = simDb.transaction
        event = simDb.event
    }

    SimResource(ActorType actorType, TransactionType transactionType, String event, String filename) {
        actor = actorType ? actorType.shortName : ResDb.BASE_TYPE
        transaction = transactionType ? transactionType.shortName : ResDb.ANY_TRANSACTION
        this.event = event
        this.filename = filename
    }

    SimResource(String _path) {
        (actor, transaction, event, filename) = _path.split(Pattern.quote(File.separator))
    }

    SimResource(File _file) {
        def path = _file.toString().split(Pattern.quote(File.separator))
        filename = path[-1]
        event = path[-2]
        transaction = path[-3]
        actor = path[-4]
    }

    String asPath() {
        File file = new File(filename)
        "${actor}${File.separator}${transaction}${File.separator}${event}${File.separator}${file.name}"
    }

    SimDb asSimDb(SimId simId) {
        SimDb simDb = new SimDb(simId, actor, transaction)
        simDb.event = event
        return simDb
    }

}
