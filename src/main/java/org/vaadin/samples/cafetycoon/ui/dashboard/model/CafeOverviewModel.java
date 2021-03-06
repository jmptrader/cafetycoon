package org.vaadin.samples.cafetycoon.ui.dashboard.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vaadin.samples.cafetycoon.domain.Cafe;
import org.vaadin.samples.cafetycoon.domain.CafeStatus;
import org.vaadin.samples.cafetycoon.domain.CafeStatusService;
import org.vaadin.samples.cafetycoon.domain.CoffeeDrink;
import org.vaadin.samples.cafetycoon.domain.CoffeeDrinkSaleStats;
import org.vaadin.samples.cafetycoon.domain.ServiceProvider;
import org.vaadin.samples.cafetycoon.domain.events.CafeStatusChangedEvent;
import org.vaadin.samples.cafetycoon.domain.events.RestockEvent;
import org.vaadin.samples.cafetycoon.domain.events.SaleEvent;
import org.vaadin.samples.cafetycoon.domain.events.StockChangeEvent;
import org.vaadin.samples.cafetycoon.domain.StockService;
import org.vaadin.samples.cafetycoon.domain.SalesService;
import org.vaadin.samples.cafetycoon.ui.utils.EventContainer;

import com.google.common.eventbus.Subscribe;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;

/**
 * Application model that contains an overview of the currently selected cafe.
 * This model is an observer of {@link CafeSelectionModel}.
 */
@SuppressWarnings("serial")
public class CafeOverviewModel extends AbstractModel implements CafeSelectionModel.Observer {

	/**
	 * Property ID for the name of the coffee drink.
	 */
	public static final String COL_DRINK = "Coffee Drink";
	/**
	 * Property ID for the price of the coffee drink.
	 */
	public static final String COL_PRICE = "Price";
	/**
	 * Property ID for the number of sold units of the coffee drink.
	 */
	public static final String COL_UNITS_SOLD = "Units Sold";
	/**
	 * Property ID for the total income generated by selling the coffee drink.
	 */
	public static final String COL_INCOME = "Income";

	private CafeSelectionModel selectionModel;
	private final ServiceProvider<StockService> stockService;
	private final ServiceProvider<SalesService> salesService;
	private final ServiceProvider<CafeStatusService> cafeStatusService;
	private final EventContainer<StockChangeEvent> beanStockChanges24h = new EventContainer<>();
	private final IndexedContainer salesData24h = createSalesDataContainer();
	private final ObjectProperty<CafeStatus> currentStatus = new ObjectProperty<>(CafeStatus.UNKNOWN, CafeStatus.class);

	/**
	 * Creates a new {@code CafeOverviewModel}. The constructor parameters are
	 * all required backend services.
	 */
	public CafeOverviewModel(ServiceProvider<StockService> stockService, ServiceProvider<SalesService> salesService,
			ServiceProvider<CafeStatusService> cafeStatusService) {
		this.stockService = stockService;
		this.salesService = salesService;
		this.cafeStatusService = cafeStatusService;
	}

	private IndexedContainer createSalesDataContainer() {
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty(COL_DRINK, String.class, null);
		container.addContainerProperty(COL_PRICE, BigDecimal.class, BigDecimal.ZERO);
		container.addContainerProperty(COL_UNITS_SOLD, Integer.class, 0);
		container.addContainerProperty(COL_INCOME, BigDecimal.class, BigDecimal.ZERO);
		return container;
	}

	/**
	 * Returns an indexed container of the sales of individual drinks during the
	 * past 24 hours.
	 * 
	 * @see #COL_DRINK
	 * @see #COL_INCOME
	 * @see #COL_PRICE
	 * @see #COL_UNITS_SOLD
	 */
	public Indexed salesData24h() {
		return salesData24h;
	}

	/**
	 * Returns an event container of the bean stock changes during the past 24
	 * hours.
	 */
	public EventContainer<StockChangeEvent> beanStockChanges24h() {
		return beanStockChanges24h;
	}

	/**
	 * Returns a property containing the current status of the cafe.
	 */
	public ObjectProperty<CafeStatus> currentStatus() {
		return currentStatus;
	}

	/**
	 * Updates the sales data and bean stock when a sale event has been
	 * registered (more units sold, more income, less beans in stock).
	 */
	@Subscribe
	protected synchronized void onSaleEvent(SaleEvent event) {
		if (event.getCafe().equals(selectionModel.selection().getValue())) {
			access(this::updateSalesData, this::updateBeanStock);
		}
	}

	/**
	 * Updates the bean stock when a restock event has been registered (more
	 * beans in stock).
	 */
	@Subscribe
	protected synchronized void onRestockEvent(RestockEvent event) {
		if (event.getCafe().equals(selectionModel.selection().getValue())) {
			access(this::updateBeanStock);
		}
	}

	/**
	 * Updates the current status when a status change event has been
	 * registered.
	 */
	@Subscribe
	protected synchronized void onCafeStatusChangedEvent(CafeStatusChangedEvent event) {
		if (event.getCafe().equals(selectionModel.selection().getValue())) {
			access(this::updateStatus);
		}
	}

	private void updateStatus() {
		Cafe cafe = selectionModel.selection().getValue();
		if (cafe != null) {
			currentStatus.setValue(cafeStatusService.get().getCurrentStatus(cafe));
		} else {
			currentStatus.setValue(CafeStatus.UNKNOWN);
		}
	}

	private void updateBeanStock() {
		Cafe cafe = selectionModel.selection().getValue();
		List<StockChangeEvent> stockChanges;
		if (cafe != null) {
			stockChanges = new ArrayList<>(stockService.get().get24hStockChangeEvents(cafe));
		} else {
			stockChanges = Collections.emptyList();
		}
		beanStockChanges24h.setEvents(stockChanges);
	}

	@SuppressWarnings("unchecked")
	private void updateSalesData() {
		Cafe cafe = selectionModel.selection().getValue();
		List<CoffeeDrinkSaleStats> saleStats;
		if (cafe != null) {
			saleStats = salesService.get().get24hSaleStats(cafe);
		} else {
			saleStats = Collections.emptyList();
		}

		Set<CoffeeDrink> drinksToDelete = new HashSet<>((Collection<CoffeeDrink>) salesData24h.getItemIds());
		saleStats.forEach(stats -> addOrUpdateSaleStats(drinksToDelete, stats));
		drinksToDelete.forEach(salesData24h::removeItem);
	}

	@SuppressWarnings("unchecked")
	private void addOrUpdateSaleStats(Set<CoffeeDrink> drinksToDelete, CoffeeDrinkSaleStats stats) {
		Item item = salesData24h.getItem(stats.getDrink());
		if (item == null) {
			item = salesData24h.addItem(stats.getDrink());
		}
		drinksToDelete.remove(stats.getDrink());
		item.getItemProperty(COL_DRINK).setValue(stats.getDrink().getName());
		item.getItemProperty(COL_PRICE).setValue(stats.getDrink().getPrice());
		item.getItemProperty(COL_UNITS_SOLD).setValue(stats.getUnitsSold());
		item.getItemProperty(COL_INCOME).setValue(stats.getTotalIncome());
	}

	@Override
	public void setCafeSelectionModel(CafeSelectionModel model) {
		if (selectionModel != null) {
			selectionModel.selection().removeValueChangeListener(this::cafeSelectionChanged);
		}
		selectionModel = model;
		if (selectionModel != null) {
			selectionModel.selection().addValueChangeListener(this::cafeSelectionChanged);
		}
	}

	private void cafeSelectionChanged(Property.ValueChangeEvent event) {
		access(this::updateBeanStock, this::updateSalesData, this::updateStatus);
	}

	/**
	 * Invokes the backend to restock the currently selected cafe. You could
	 * argue whether this is the correct place for this method. The following
	 * factors advocates this decision:
	 * <ul>
	 * <li>the model already has access to the necessary backend service for
	 * other purposes</li>
	 * <li>the restock operation is invoked from the same view that observes
	 * this model</li>
	 * <li>the restock operation requires the current cafe, which this model is
	 * aware of</li>
	 * </ul>
	 */
	public void restock(int amount) {
		Cafe cafe = selectionModel.selection().getValue();
		if (cafe != null) {
			stockService.get().restock(cafe, new BigDecimal(amount));
		}
	}

	/**
	 * Convenience interface for model observers, not a requirement.
	 */
	public interface Observer {
		void setCafeOverviewModel(CafeOverviewModel model);
	}
}
