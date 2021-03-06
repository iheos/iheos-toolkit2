package gov.nist.toolkit.xdstools2.client.tabs.simulatorControlTab;

import com.google.gwt.user.client.ui.FlowPanel;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.xdstools2.client.command.command.GetSimConfigsCommand;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.shared.command.request.GetSimConfigsRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulator configuration supervisor - holds multiple SimConfigMgr objects, each one manages
 * the details for a single simulator.
 * @author bill
 *
 */
class SimConfigSuper {
	FlowPanel panel;
	List<SimConfigMgr> mgrs = new ArrayList<>();
	SimulatorControlTab simulatorControlTab;
	String testSession;

	SimConfigSuper(SimulatorControlTab simulatorControlTab, FlowPanel panel, String testSession) {
		this.simulatorControlTab = simulatorControlTab;
		this.panel = panel;
		this.testSession = testSession;
	}

	List<SimId> getIds() {
		List<SimId> ids = new ArrayList<>();
		for (SimConfigMgr mgr : mgrs) {
			ids.add(mgr.config.getId());
		}
		return ids;
	}

	void add(SimulatorConfig config) {
		delete(config);
		SimConfigMgr mgr = new SimConfigMgr(simulatorControlTab, panel, config, testSession);
		mgr.displayBasicSimulatorConfig();
		mgr.displayInPanel();
		mgrs.add(mgr);

		String txt = simulatorControlTab.simIdsTextArea.getText();
		if (txt == null || txt.equals("")) {
			simulatorControlTab.simIdsTextArea.setText(mgr.config.getId().toString());
		} else {
			txt = txt + ", " + mgr.config.getId();
			simulatorControlTab.simIdsTextArea.setText(txt);
		}
	}

	/*
	 * Delete existing instance of this simulator
	 */
	void delete(SimulatorConfig config) {
		String targetId = config.getId().toString();
		SimConfigMgr toDelete = null;
		for (SimConfigMgr mgr : mgrs) {
			String id = mgr.config.getId().toString();
			if (id.equals(targetId)) {
				toDelete = mgr;
				break;
			}
		}
		if (toDelete != null)
			mgrs.remove(toDelete);

	}

	void clear() {
		mgrs.clear();
		panel.clear();
	}

	void refresh() {
		panel.clear();

		String txt = "";
		for (SimConfigMgr mgr : mgrs) {
			if (txt.equals(""))
				txt = mgr.config.getId().toString();
			else
				txt = txt + ", " + mgr.config.getId();

			mgr.removeFromPanel();
			mgr.displayBasicSimulatorConfig();
			mgr.displayInPanel();
		}
		simulatorControlTab.simIdsTextArea.setText(txt);

//		simulatorControlTab.updateSimulatorCookies(txt);
	}

	void reloadSimulators() {
		new GetSimConfigsCommand(){
			@Override
			public void onComplete(List<SimulatorConfig> configs) {
				simulatorControlTab.simIdsTextArea.setText("");
				clear();
				for (SimulatorConfig config : configs)
					add(config);

				refresh();
			}
		}.run(new GetSimConfigsRequest(ClientUtils.INSTANCE.getCommandContext(),getIds()));
	}

}
