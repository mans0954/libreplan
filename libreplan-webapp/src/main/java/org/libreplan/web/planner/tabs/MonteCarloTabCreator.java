/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.libreplan.web.planner.tabs;

import static org.libreplan.web.I18nHelper._;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.BREADCRUMBS_SEPARATOR;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.getSchedulingLabel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.joda.time.LocalDate;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.Registry;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.TaskSource;
import org.libreplan.business.planner.entities.Dependency;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.web.common.TemplateModel.DependencyWithVisibility;
import org.libreplan.web.common.TemplateModelAdapter;
import org.libreplan.web.montecarlo.MonteCarloController;
import org.libreplan.web.planner.order.OrderPlanningController;
import org.libreplan.web.planner.order.PlanningStateCreator;
import org.libreplan.web.planner.order.PlanningStateCreator.IActionsOnRetrieval;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.libreplan.web.planner.tabs.CreatedOnDemandTab.IComponentCreator;
import org.zkoss.ganttz.adapters.PlannerConfiguration;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.GanttDiagramGraph;
import org.zkoss.ganttz.data.GanttDiagramGraph.IAdapter;
import org.zkoss.ganttz.data.constraint.Constraint;
import org.zkoss.ganttz.data.criticalpath.CriticalPathCalculator;
import org.zkoss.ganttz.extensions.ITab;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

/**
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
public class MonteCarloTabCreator {

    private String ORDER_LIMITING_RESOURCES_VIEW = _("MonteCarlo Method");

    public static ITab create(Mode mode,
            PlanningStateCreator planningStateCreator,
            MonteCarloController monteCarloController,
            OrderPlanningController orderPlanningController,
            Component breadcrumbs, IResourcesSearcher resourcesSearcher) {

        return new MonteCarloTabCreator(mode, planningStateCreator,
                monteCarloController, orderPlanningController, breadcrumbs,
                resourcesSearcher)
                .build();
    }

    private final Mode mode;

    private final MonteCarloController monteCarloController;

    private final OrderPlanningController orderPlanningController;

    private final Component breadcrumbs;

    private final IResourcesSearcher resourcesSearcher;

    private final PlanningStateCreator planningStateCreator;

    private MonteCarloTabCreator(Mode mode,
            PlanningStateCreator planningStateCreator,
            MonteCarloController MonteCarloController,
            OrderPlanningController orderPlanningController,
            Component breadcrumbs, IResourcesSearcher resourcesSearcher) {
        Validate.notNull(resourcesSearcher);
        Validate.notNull(planningStateCreator);
        this.planningStateCreator = planningStateCreator;
        this.mode = mode;
        this.monteCarloController = MonteCarloController;
        this.orderPlanningController = orderPlanningController;
        this.breadcrumbs = breadcrumbs;
        this.resourcesSearcher = resourcesSearcher;
    }

    private ITab build() {
        return TabOnModeType.forMode(mode)
                .forType(ModeType.GLOBAL, createGlobalMonteCarloTab())
                .forType(ModeType.ORDER, createOrderMonteCarloTab())
                .create();
    }

    private ITab createOrderMonteCarloTab() {

        IComponentCreator componentCreator = new IComponentCreator() {

            @Override
            public org.zkoss.zk.ui.Component create(
                    org.zkoss.zk.ui.Component parent) {

                Map<String, Object> arguments = new HashMap<String, Object>();
                arguments.put("monteCarloController",
                        monteCarloController);
                return Executions.createComponents(
                        "/montecarlo/_montecarlo.zul", parent, arguments);
            }

        };

        return new CreatedOnDemandTab(ORDER_LIMITING_RESOURCES_VIEW,
                "order-limiting-resources", componentCreator) {

            @Override
            protected void afterShowAction() {
                List<TaskElement> criticalPath = orderPlanningController.getCriticalPath();
                if (criticalPath == null) {
                    criticalPath = getCriticalPath(mode.getOrder(),
                            getDesktop());
                }
                monteCarloController.setCriticalPath(criticalPath);

                breadcrumbs.getChildren().clear();
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(getSchedulingLabel()));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs
                        .appendChild(new Label(ORDER_LIMITING_RESOURCES_VIEW));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(mode.getOrder().getName()));
            }

        };
    }

    List<TaskElement> getCriticalPath(final Order order, final Desktop desktop) {
        IAdHocTransactionService transactionService = Registry
                .getTransactionService();
        return transactionService
                .runOnTransaction(new IOnTransaction<List<TaskElement>>() {
                    public List<TaskElement> execute() {
                        PlanningState state = retrieveOrCreate();
                        return getCriticalPathFor(state.getCurrentScenario(),
                                state.getOrder());
                    }

                    private PlanningState retrieveOrCreate() {
                        return planningStateCreator.retrieveOrCreate(desktop,
                                order, new IActionsOnRetrieval() {

                                    @Override
                                    public void onRetrieval(
                                            PlanningState planningState) {
                                        planningState.reattach();
                                    }
                                });
                    }
                });
    }

    /**
     * Calculate critical path tasks in order
     *
     * To calculate the tasks that are in the critical path is necesary to
     * create an empy graph filled with the tasks and dependencies of this order
     *
     * @param order
     * @return
     */
    public List<TaskElement> getCriticalPathFor(Scenario currentScenario,
            Order order) {
        CriticalPathCalculator<TaskElement, DependencyWithVisibility> criticalPathCalculator = CriticalPathCalculator
                .create(order.getDependenciesConstraintsHavePriority());
        IAdapter<TaskElement, DependencyWithVisibility> adapter = TemplateModelAdapter
                .create(currentScenario, asLocalDate(order.getInitDate()),
                        asLocalDate(order.getDeadline()), resourcesSearcher);
        GanttDiagramGraph<TaskElement, DependencyWithVisibility> graph = createFor(
                order, adapter);
        graph.addTasks(order.getAllChildrenAssociatedTaskElements());
        addDependencies(graph, order);
        return criticalPathCalculator.calculateCriticalPath(graph);
    }

    private LocalDate asLocalDate(Date date) {
        return date != null ? LocalDate.fromDateFields(date) : null;
    }

    private void addDependencies(
                    GanttDiagramGraph<TaskElement, DependencyWithVisibility> graph,
            Order order) {
        for (Dependency each : getAllDependencies(order)) {
            graph.addWithoutEnforcingConstraints(DependencyWithVisibility
                    .existent(each));
        }
            }

    private Set<Dependency> getAllDependencies(Order order) {
        Set<Dependency> dependencies = new HashSet<Dependency>();
        for (TaskElement each : getTaskElementsFrom(order)) {
                    Set<Dependency> dependenciesWithThisOrigin = each
                    .getDependenciesWithThisOrigin();
                    dependencies.addAll(dependenciesWithThisOrigin);
        }
        return dependencies;
            }

    private List<TaskElement> getTaskElementsFrom(Order order) {
                List<TaskElement> result = new ArrayList<TaskElement>();
        for (TaskSource each : order.getTaskSourcesFromBottomToTop()) {
            result.add(each.getTask());
                }
        return result;
    }

    private GanttDiagramGraph<TaskElement, DependencyWithVisibility> createFor(
            Order order, IAdapter<TaskElement, DependencyWithVisibility> adapter) {
        GanttDate orderStart = GanttDate.createFrom(order.getInitDate());
        List<Constraint<GanttDate>> startConstraints = PlannerConfiguration
                .getStartConstraintsGiven(orderStart);
        GanttDate deadline = GanttDate.createFrom(order.getDeadline());
        List<Constraint<GanttDate>> endConstraints = PlannerConfiguration
                .getEndConstraintsGiven(deadline);
        GanttDiagramGraph<TaskElement, DependencyWithVisibility> result = GanttDiagramGraph
                .create(order.isScheduleBackwards(), adapter, startConstraints,
                        endConstraints,
                        order.getDependenciesConstraintsHavePriority());
        return result;
    }


    private ITab createGlobalMonteCarloTab() {

        final IComponentCreator componentCreator = new IComponentCreator() {

            @Override
            public org.zkoss.zk.ui.Component create(
                    org.zkoss.zk.ui.Component parent) {
                // do nothing
                return null;
            }

        };
        return new CreatedOnDemandTab(_("MonteCarlo Method"),
                "montecarlo-simulation", componentCreator) {
            @Override
            protected void afterShowAction() {
                // do nothing
            }
        };
    }

}