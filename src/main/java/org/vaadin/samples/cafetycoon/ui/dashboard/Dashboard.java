package org.vaadin.samples.cafetycoon.ui.dashboard;

import org.vaadin.samples.cafetycoon.domain.Services;
import org.vaadin.samples.cafetycoon.ui.dashboard.model.CafeOverviewModel;
import org.vaadin.samples.cafetycoon.ui.dashboard.model.CafeSelectionModel;
import org.vaadin.samples.cafetycoon.ui.dashboard.model.SalesOverviewModel;
import org.vaadin.samples.cafetycoon.ui.utils.TitledElement;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;

@SuppressWarnings("serial")
public class Dashboard extends DashboardDesign implements View, TitledElement {

	public static final String VIEW_NAME = "dashboard";

	private SalesOverviewModel salesOverviewModel;
	private CafeSelectionModel cafeSelectionModel;
	private CafeOverviewModel cafeOverviewModel;

	public Dashboard() {
		// Since we're not using a dependency injection framework, we have to
		// wire up everything manually. In this case, we're simply using method
		// pointers as service providers.
		// In a real application, you would probably want to use more advanced
		// service providers that serialize and deserialize properly, check for
		// the availability of the backend service, etc.

		Services s = Services.getInstance();

		salesOverviewModel = new SalesOverviewModel(s::getCafeStatusService, s::getSalesService, s::getBalanceService,
				s::getCafeRepository);
		salesOverview.setSalesOverviewModel(salesOverviewModel);
		cafeMap.setSalesOverviewModel(salesOverviewModel);

		cafeSelectionModel = new CafeSelectionModel();
		salesOverview.setCafeSelectionModel(cafeSelectionModel);
		cafeMap.setCafeSelectionModel(cafeSelectionModel);
		cafeOverview.setCafeSelectionModel(cafeSelectionModel);

		cafeOverviewModel = new CafeOverviewModel(s::getStockService, s::getSalesService, s::getCafeStatusService);
		cafeOverviewModel.setCafeSelectionModel(cafeSelectionModel);
		cafeOverview.setCafeOverviewModel(cafeOverviewModel);
	}

	@Override
	public void attach() {
		super.attach();
		salesOverviewModel.attach(getUI(), Services.getInstance()::getEventBus);
		cafeOverviewModel.attach(getUI(), Services.getInstance()::getEventBus);
	}

	@Override
	public void detach() {
		cafeOverviewModel.detach();
		salesOverviewModel.detach();
		super.detach();
	}

	@Override
	public void enter(ViewChangeEvent event) {
		// NOP
	}

	@Override
	public String getTitle() {
		return "Dashboard";
	}
}
