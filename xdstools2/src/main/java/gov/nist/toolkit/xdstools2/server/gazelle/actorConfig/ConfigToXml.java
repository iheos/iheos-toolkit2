package gov.nist.toolkit.xdstools2.server.gazelle.actorConfig;

import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.sitemanagement.SeparateSiteLoader;
import gov.nist.toolkit.sitemanagement.Sites;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import org.apache.axiom.om.OMElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigToXml {

	static final int ReposSystemName = 0;
	static final int ReposRepUid = 1;


	Map<String, List<GazelleEntry>> gMap = new HashMap<String, List<GazelleEntry>>();

	File actorsDir;
	GazelleConfigs gConfigs;
	OidConfigs oConfigs;
	HashMap<String, String> repIdMap = new HashMap<String, String>();
	
	// Supports junit testing
	public Site lastSite = null;

	public ConfigToXml(GazelleConfigs gConfigs, OidConfigs oConfigs, File actorsDir)  {
		this.gConfigs = gConfigs;
		this.oConfigs = oConfigs;
		this.actorsDir = actorsDir;
	}

	public String process() throws IOException {
		StringBuffer conflictBuffer = new StringBuffer();
		int siteCount = 0;
		int ignoredSiteCount = 0;
		buildSystemMap();

		Sites sites = new Sites(TestSession.DEFAULT_TEST_SESSION);
		for (String sysName : gMap.keySet()) {
			if (sysName == null || sysName.equals(""))
				continue;
			String actorName = sysName; 
			Site site = buildSite(actorName, gMap.get(sysName), conflictBuffer);
			lastSite = site;
			if (site == null) {
				conflictBuffer.append("Entry for system <" + sysName + "> not loaded\n");
				System.out.println("Entry for system <" + sysName + "> not loaded\n");
				ignoredSiteCount++;
				continue;
			}
			siteCount++;
			OMElement siteXml = new SeparateSiteLoader(TestSession.DEFAULT_TEST_SESSION).siteToXML(site);
			if (actorsDir != null) {
				File outfile = new File(actorsDir + File.separator + actorName + ".xml");
				Io.xmlToFile(outfile, siteXml);
			}
		}
		
		conflictBuffer.insert(0, "Loaded " + siteCount + " Sites\nIgnored " + ignoredSiteCount + " Sites (non-XD* or not approved)\n\n");
		
		return conflictBuffer.toString();

	}

	String systemNameAsActorName(String sysName) {
		return sysName.replaceAll(" ", "_").replaceAll(",", "");
	}

	void buildSystemMap() {
		for (int i=0; i<gConfigs.size(); i++) {
			GazelleEntry entry = gConfigs.get(i);
			String system = gConfigs.getSystem(i);
			List<GazelleEntry> entries = gMap.get(system);
			if (entries == null) {
				entries = new ArrayList<GazelleEntry>();
				gMap.put(system, entries);
			}
			entries.add(entry);
		}
	}

	Site buildSite(String sysname, List<GazelleEntry> entries, StringBuffer conflicts) {
		boolean homeGenerated = false;
		
		if (entries.size() == 0)
			return null;

		Site site = new Site(sysname, TestSession.DEFAULT_TEST_SESSION);

		List<SysConfig> configs = new ArrayList<SysConfig>();

		System.out.println("Building Site for " + sysname);
		for (GazelleEntry entry : entries) {
			System.out.println(entry);
			SysConfig sysConfig = new SysConfig(this, entry);
			boolean ok = sysConfig.eval(this);
			if (!ok)
				continue;

			System.out.println(sysConfig);
			TransactionType trans = sysConfig.trans;
			
//			if (!sysConfig.sysName.equals("XDSb_REG_SER_HealthCare"))
//				continue;
			
			if (sysConfig.isAsync)
				continue;
			
//			if (!sysConfig.isApproved)
//				continue;

			configs.add(sysConfig);

			System.out.println("transId is " + entry.getTransId() + " trans is " + trans);
			if (trans == null)
				continue;
			
			// These actor types are black-balled for now
//			if (sysConfig.actorType == ActorType.ONDEMAND_DOCUMENT_SOURCE)
//				continue;
			
			site.addTransaction(sysConfig.getTransactionBean());

			if (
					sysConfig.trans.getId().equals("ITI-43") && 
					sysConfig.repUid != null && 
					!sysConfig.repUid.equals("") &&
					sysConfig.actorType == ActorType.REPOSITORY) {
				site.addRepository(sysConfig.getRepositoryBean());
			}
			
			if (
					sysConfig.trans.getId().equals("ITI-38") && 
					sysConfig.home != null && 
					!sysConfig.home.equals("") && 
					!homeGenerated) {
				site.setHome(sysConfig.home);
				homeGenerated = true;

			}

		}

		if (site.size() == 0)
			return null;
		
		site.validate(conflicts);
		
		return site;
	}


	public static void main(String[] args) {
		File actorsDir = new File("/Users/bill/tmp/toolkit2/actors");
		String systemName = "ALL";   // "ALL";
		GazelleConfigs gConfigs = null; 
		OidConfigs oConfigs = null;

		try {
			//
			// Load OIDs table
			//
			oConfigs = new OidConfigs();
			new CSVParser(new OidEntryFactory())
			.parse(oConfigs, Io.stringFromFile(new File(actorsDir + File.separator + "ListOID.csv")));

			if (systemName.equals("ALL")) {
				// 
				// Parse all entries in Gazelle configuration - downloaded as a single file
				//
				gConfigs = new GazelleConfigs();
				new CSVParser(new GazelleEntryFactory())
				.parse(gConfigs, Io.stringFromFile(new File(actorsDir + File.separator + "all.csv")));

				//
				// Generate actor files
				new ConfigToXml(
						gConfigs, 
						oConfigs, 
						actorsDir)
				.process();
			}
			else {
				//
				// Parse single entry identified as systemName.  The raw CSV has already been downloaded
				//
				gConfigs = new GazelleConfigs();
				new CSVParser(new GazelleEntryFactory())
				.parse(gConfigs, Io.stringFromFile(new File(actorsDir + File.separator + systemName + ".csv")));

				//
				// Generate actor files
				new ConfigToXml(gConfigs, oConfigs, actorsDir).process();
			}
		} catch (Exception e) {
			System.out.println(ExceptionUtil.exception_details(e));
		}

	}

}
