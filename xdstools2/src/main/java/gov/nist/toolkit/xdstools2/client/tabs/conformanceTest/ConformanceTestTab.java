package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.session.client.SectionOverviewDTO;
import gov.nist.toolkit.session.client.TestOverviewDTO;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.testkitutilities.client.TestCollectionDefinitionDAO;
import gov.nist.toolkit.xdstools2.client.HorizontalFlowPanel;
import gov.nist.toolkit.xdstools2.client.PopupMessage;
import gov.nist.toolkit.xdstools2.client.ToolWindow;
import gov.nist.toolkit.xdstools2.client.Xdstools2;
import gov.nist.toolkit.xdstools2.client.event.TestSessionChangedEvent;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionChangedEventHandler;
import gov.nist.toolkit.xdstools2.client.inspector.MetadataInspectorTab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConformanceTestTab extends ToolWindow implements TestRunner {
//	final protected ToolkitServiceAsync toolkitService = GWT
//			.create(ToolkitService.class);

	private final ConformanceTestTab me;
	private final FlowPanel toolPanel = new FlowPanel();   // Outer-most panel for the tool
	private final FlowPanel testsPanel = new FlowPanel();  // panel for displaying tests
	private final TabBar tabBar = new TabBar();            // tab bar at the top for selecting actor types
	private final FlowPanel sitesPanel = new FlowPanel();
	private String currentActorTypeName;
	private String currentSiteName = null;

	// Testable actors
	private List<TestCollectionDefinitionDAO> testCollectionDefinitionDAOs;
	// testId ==> overview
	private final Map<String, TestOverviewDTO> testOverviews = new HashMap<>();

	public ConformanceTestTab() {
		me = this;
		toolPanel.add(sitesPanel);
		toolPanel.add(tabBar);
		toolPanel.add(testsPanel);
	}

	@Override
	public String getTitle() { return "Conformance Tests"; }

	@Override
	public void onTabLoad(boolean select, String eventName) {

		addEast(new HTML("Test Session"));

		registerTab(select, eventName);

		tabTopPanel.add(toolPanel);


		// Reload if the test session changes
		Xdstools2.getEventBus().addHandler(TestSessionChangedEvent.TYPE, new TestSessionChangedEventHandler() {
			@Override
			public void onTestSessionChanged(TestSessionChangedEvent event) {
				if (event.changeType == TestSessionChangedEvent.ChangeType.SELECT) {
					loadTestCollections();
				}
			}
		});

		// List of sites
		buildSiteSelector();

		tabBar.addSelectionHandler(actorSelectionHandler);

		// Initial load of tests in a test session
		loadTestCollections();
	}

	// actor selection changes
	private SelectionHandler<Integer> actorSelectionHandler = new SelectionHandler<Integer>() {
		@Override
		public void onSelection(SelectionEvent<Integer> selectionEvent) {
			int i = selectionEvent.getSelectedItem();
			currentActorTypeName = testCollectionDefinitionDAOs.get(i).getCollectionID();
			loadTestCollection(currentActorTypeName);
		}
	};

	// selection of site to be tested
	private void buildSiteSelector() {
		SiteSelectionComponent siteSelectionComponent = new SiteSelectionComponent(null, getCurrentTestSession());
		siteSelectionComponent.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> siteSelected) {
				currentSiteName = siteSelected.getValue();
			}
		});
		sitesPanel.add(siteSelectionComponent.asWidget());
	}

	// update things when site selection changes
	private ValueChangeHandler siteChangeHandler = new ValueChangeHandler() {
		@Override
		public void onValueChange(ValueChangeEvent valueChangeEvent) {

		}
	};

	private String getSelectedTestCollection() {
		int selected = tabBar.getSelectedTab();
		if (selected < testCollectionDefinitionDAOs.size()) {
			String name = testCollectionDefinitionDAOs.get(selected).getCollectionID();
			return name;
		}
		return null;
	}

	private int getTestCollectionIndex(String name) {
		int i = 0;
		for (TestCollectionDefinitionDAO def : testCollectionDefinitionDAOs) {
			if (def.getCollectionID().equals(name))
				return i;
			i++;
		}
		return -1;
	}

	// load tab bar with actor types
	private void loadTestCollections() {
		// TabBar listing actor types
		toolkitService.getTestCollections("actorCollections", new AsyncCallback<List<TestCollectionDefinitionDAO>>() {
			@Override
			public void onFailure(Throwable throwable) { new PopupMessage("getTestCollections: " + throwable.getMessage()); }

			@Override
			public void onSuccess(List<TestCollectionDefinitionDAO> testCollectionDefinitionDAOs) {
				me.testCollectionDefinitionDAOs = testCollectionDefinitionDAOs;
				displayTestCollectionsTabBar();
			}
		});
	}

	// load test results for a single test collection (actor type) for a single site
	private void loadTestCollection(final String collectionName) {
		testDisplays.clear();  // so they reload

		// what tests are in the collection
		toolkitService.getCollectionMembers("actorcollections", collectionName, new AsyncCallback<List<String>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getTestlogListing: " + caught.getMessage());
			}

			public void onSuccess(List<String> testIds) {
				testsPanel.clear();
				List<TestInstance> testInstances = new ArrayList<>();
				for (String testId : testIds) testInstances.add(new TestInstance(testId));

				// results (including logs) for a collection of tests
				toolkitService.getTestsOverview(getCurrentTestSession(), testInstances, new AsyncCallback<List<TestOverviewDTO>>() {

					public void onFailure(Throwable caught) {
						new PopupMessage("getTestOverview: " + caught.getMessage());
					}

					public void onSuccess(List<TestOverviewDTO> testOverviews) {
						testsPanel.clear();
						for (TestOverviewDTO testOverview : testOverviews) {
							me.testOverviews.put(testOverview.getName(), testOverview);
							displayTest(testOverview);
						}
					}

				});
			}
		});

	}

	private void displayTestCollectionsTabBar() {
		if (tabBar.getTabCount() == 0) {
			for (TestCollectionDefinitionDAO def : testCollectionDefinitionDAOs) {
				tabBar.addTab(def.getCollectionTitle());
			}
		}
	}

	// header and body per test
	// key is testOverview.getName()
	private Map<String, TestDisplay> testDisplays = new HashMap<>();
	private class TestDisplay {
		HorizontalFlowPanel header = new HorizontalFlowPanel();
		FlowPanel body = new FlowPanel();
		DisclosurePanel panel = new DisclosurePanel(header);

		TestDisplay(String name) {
			header.fullWidth();
			panel.setWidth("100%");
			panel.add(body);
			testDisplays.put(name, this);
		}
	}

	private boolean testDisplayExists(String name) { return testDisplays.containsKey(name); }

	private TestDisplay buildTestDisplay(String name) {
		if (testDisplayExists(name)) return testDisplays.get(name);
		return new TestDisplay(name);
	}

	private void displayTest(TestOverviewDTO testOverview) {
		boolean isNew = !testDisplayExists(testOverview.getName());
		TestDisplay testDisplay = buildTestDisplay(testOverview.getName());
		HorizontalFlowPanel header = testDisplay.header;
		FlowPanel body = testDisplay.body;

		if (isNew) {
			testsPanel.add(testDisplay.panel);
		}

		header.clear();
		body.clear();

		if (testOverview.isRun()) {
			if (testOverview.isPass()) header.setBackgroundColorSuccess();
			else header.setBackgroundColorFailure();
		} else header.setBackgroundColorNotRun();

		HTML testHeader = new HTML("Test: " + testOverview.getName() + " - " +testOverview.getTitle());
		testHeader.addStyleName("test-title");
		header.add(testHeader);
		if (testOverview.isRun()) {
			Image status = (testOverview.isPass()) ?
					new Image("icons2/correct-24.png")
					:
					new Image("icons/ic_warning_black_24dp_1x.png");
			status.addStyleName("right");
			header.add(status);
		}

		Image play = new Image("icons2/play-24.png");
		play.setTitle("Run");
		play.addClickHandler(new RunClickHandler(testOverview.getTestInstance()));
		header.add(play);
		if (testOverview.isRun()) {
			Image delete = new Image("icons2/garbage-24.png");
			delete.addStyleName("right");
			delete.addClickHandler(new DeleteClickHandler(testOverview.getTestInstance()));
			delete.setTitle("Delete Log");
			header.add(delete);

			Image inspect = new Image("icons2/visible-32.png");
			inspect.addStyleName("right");
			inspect.addClickHandler(new InspectClickHandler(testOverview.getTestInstance()));
			inspect.setTitle("Inspect results");
			header.add(inspect);
		}

		body.add(new HTML(testOverview.getDescription()));

		// display sections within test
		displaySections(testOverview, body);
	}

	private class RunClickHandler implements ClickHandler {
		TestInstance testInstance;

		RunClickHandler(TestInstance testInstance) {
			this.testInstance = testInstance;
		}

		@Override
		public void onClick(ClickEvent clickEvent) {
			runTest(testInstance);
		}
	}

	private class DeleteClickHandler implements ClickHandler {
		TestInstance testInstance;

		DeleteClickHandler(TestInstance testInstance) {
			this.testInstance = testInstance;
		}

		@Override
		public void onClick(ClickEvent clickEvent) {
			toolkitService.deleteSingleTestResult(getCurrentTestSession(), testInstance, new AsyncCallback<TestOverviewDTO>() {
				@Override
				public void onFailure(Throwable throwable) {
					new PopupMessage(throwable.getMessage());
				}

				@Override
				public void onSuccess(TestOverviewDTO testOverviewDTO) {
					displayTest(testOverviewDTO);
				}
			});
		}
	}

	private class InspectClickHandler implements ClickHandler {
		TestInstance testInstance;

		InspectClickHandler(TestInstance testInstance) {
			this.testInstance = testInstance;
		}

		@Override
		public void onClick(ClickEvent clickEvent) {
			List<TestInstance> testInstances = new ArrayList<>();
			testInstances.add(testInstance);
			toolkitService.getTestResults(testInstances, getCurrentTestSession(), new AsyncCallback<Map<String, Result>>() {
				@Override
				public void onFailure(Throwable throwable) {
					new PopupMessage(throwable.getMessage());
				}

				@Override
				public void onSuccess(Map<String, Result> resultMap) {
					MetadataInspectorTab itab = new MetadataInspectorTab();
					itab.setResults(resultMap.values());
					itab.setSiteSpec(new SiteSpec(currentSiteName));
//					itab.setToolkitService(me.toolkitService);
					itab.onTabLoad(true, "Insp");
				}
			});
		}
	}

	// display sections within test
	private void displaySections(TestOverviewDTO testOverview, FlowPanel parent) {
//		parent.clear();
		for (String sectionName : testOverview.getSectionNames()) {
			SectionOverviewDTO sectionOverview = testOverview.getSectionOverview(sectionName);
			parent.add(new TestSectionComponent(/*toolkitService, */getCurrentTestSession(), new TestInstance(testOverview.getName()), sectionOverview, this).asWidget());
		}
	}

	// if testInstance contains a sectionName then run that section, otherwise run entire test.
	public void runTest(final TestInstance testInstance) {
		Map<String, String> parms = new HashMap<>();
		parms.put("$patientid$", "P20160803215512.2^^^&1.3.6.1.4.1.21367.2005.13.20.1000&ISO");

		try {
			toolkitService.runTest(getEnvironmentSelection(), getCurrentTestSession(), new SiteSpec(currentSiteName), testInstance, parms, true, new AsyncCallback<TestOverviewDTO>() {
				@Override
				public void onFailure(Throwable throwable) {
					new PopupMessage(throwable.getMessage());
				}

				@Override
				public void onSuccess(TestOverviewDTO testOverviewDTO) {
					// returned status of entire test
					displayTest(testOverviewDTO);
				}
			});
		} catch (Exception e) {
			new PopupMessage(e.getMessage());
		}

	}



	public String getWindowShortName() {
		return "testloglisting";
	}
}