package org.navalplanner.business.test.orders.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.navalplanner.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.navalplanner.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Resource;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.navalplanner.business.IDataBootstrap;
import org.navalplanner.business.advance.bootstrap.PredefinedAdvancedTypes;
import org.navalplanner.business.advance.entities.AdvanceMeasurement;
import org.navalplanner.business.advance.entities.AdvanceMeasurementComparator;
import org.navalplanner.business.advance.entities.AdvanceType;
import org.navalplanner.business.advance.entities.DirectAdvanceAssignment;
import org.navalplanner.business.advance.entities.IndirectAdvanceAssignment;
import org.navalplanner.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.navalplanner.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.orders.entities.OrderLine;
import org.navalplanner.business.orders.entities.OrderLineGroup;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link OrderElement}. <br />
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
@Transactional
public class OrderElementTest {

    @Resource
    private IDataBootstrap defaultAdvanceTypesBootstrapListener;

    @Before
    public void loadRequiredaData() {
        defaultAdvanceTypesBootstrapListener.loadRequiredData();
    }

    private static OrderLine givenOrderLine(String name, String code,
            Integer hours) {
        OrderLine orderLine = OrderLine
                .createOrderLineWithUnfixedPercentage(hours);
        orderLine.setName(name);
        orderLine.setCode(code);

        return orderLine;
    }

    private static OrderLineGroup givenOrderLineGroupWithOneOrderLine(
            Integer hours) {
        OrderLineGroup orderLineGroup = OrderLineGroup.create();
        orderLineGroup.setName("OrderLineGroup1");
        orderLineGroup.setCode("1");

        OrderLine orderLine = givenOrderLine("OrderLine1", "1.1", hours);
        orderLineGroup.add(orderLine);

        return orderLineGroup;
    }

    public static OrderLineGroup givenOrderLineGroupWithTwoOrderLines(
            Integer hours1, Integer hours2) {
        OrderLineGroup orderLineGroup = givenOrderLineGroupWithOneOrderLine(hours1);

        OrderLine orderLine = givenOrderLine("OrderLine2", "1.2", hours2);
        orderLineGroup.add(orderLine);

        return orderLineGroup;
    }

    private static DirectAdvanceAssignment givenAdvanceAssigement(
            BigDecimal maxValue, AdvanceType advanceType) {
        DirectAdvanceAssignment advanceAssignment = DirectAdvanceAssignment
                .create();
        advanceAssignment.setMaxValue(maxValue);
        advanceAssignment.setAdvanceType(advanceType);
        advanceAssignment.setReportGlobalAdvance(false);

        return advanceAssignment;
    }

    public static void addAvanceAssignmentWithoutMeasurement(
            OrderElement orderElement, AdvanceType advanceType,
            BigDecimal maxValue, boolean reportGlobalAdvance)
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        DirectAdvanceAssignment advanceAssignment = givenAdvanceAssigement(
                maxValue, advanceType);
        advanceAssignment.setReportGlobalAdvance(reportGlobalAdvance);
        orderElement.addAdvanceAssignment(advanceAssignment);
    }

    public static void addAvanceAssignmentWithMeasurement(
            OrderElement orderElement, AdvanceType advanceType,
            BigDecimal maxValue, BigDecimal currentValue,
            boolean reportGlobalAdvance)
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        AdvanceMeasurement advanceMeasurement = AdvanceMeasurement.create();
        advanceMeasurement.setDate(new LocalDate());
        advanceMeasurement.setValue(currentValue);

        DirectAdvanceAssignment advanceAssignment = givenAdvanceAssigement(
                maxValue, advanceType);
        advanceAssignment.getAdvanceMeasurements().add(advanceMeasurement);
        advanceAssignment.setReportGlobalAdvance(reportGlobalAdvance);

        advanceMeasurement.setAdvanceAssignment(advanceAssignment);

        orderElement.addAdvanceAssignment(advanceAssignment);
    }

    private static AdvanceType givenAdvanceType(String name) {
        BigDecimal value = new BigDecimal(5000).setScale(2);
        BigDecimal precision = new BigDecimal(10).setScale(2);
        AdvanceType advanceType = AdvanceType.create(name, value, true,
                precision, true, false);
        return advanceType;
    }

    @Test
    public void checkAdvancePercentageEmptyOrderLine() {
        OrderLine orderLine = givenOrderLine("name", "code", 1000);
        assertThat(orderLine.getAdvancePercentage(), equalTo(BigDecimal.ZERO));
    }

    @Test
    public void checkAdvancePercentageOrderLineWithAdvanceAssignmentWithoutMesaurement()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderLine orderLine = givenOrderLine("name", "code", 1000);

        DirectAdvanceAssignment advanceAssignment = givenAdvanceAssigement(
                new BigDecimal(5000), PredefinedAdvancedTypes.UNITS.getType());

        orderLine.addAdvanceAssignment(advanceAssignment);

        assertThat(orderLine.getAdvancePercentage(), equalTo(BigDecimal.ZERO));
    }

    @Test
    public void checkAdvancePercentageOrderLineWithTwoAssignments1()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderLine orderLine = givenOrderLine("name", "code", 1000);

        addAvanceAssignmentWithMeasurement(orderLine,
                givenAdvanceType("test1"), new BigDecimal(2000),
                new BigDecimal(200), true);

        addAvanceAssignmentWithMeasurement(orderLine,
                givenAdvanceType("test2"), new BigDecimal(1000),
                new BigDecimal(600), false);

        assertThat(orderLine.getAdvancePercentage(), equalTo(new BigDecimal(10)
                .divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderLineWithTwoAssignments2()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderLine orderLine = givenOrderLine("name", "code", 1000);

        addAvanceAssignmentWithMeasurement(orderLine,
                givenAdvanceType("test1"), new BigDecimal(2000),
                new BigDecimal(200), false);

        addAvanceAssignmentWithMeasurement(orderLine,
                givenAdvanceType("test2"), new BigDecimal(1000),
                new BigDecimal(600), true);

        assertThat(orderLine.getAdvancePercentage(), equalTo(new BigDecimal(60)
                .divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderLineWithThreeAssignments()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderLine orderLine = givenOrderLine("name", "code", 1000);

        addAvanceAssignmentWithMeasurement(orderLine,
                givenAdvanceType("test1"), new BigDecimal(2000),
                new BigDecimal(200), false);

        addAvanceAssignmentWithMeasurement(orderLine,
                givenAdvanceType("test3"), new BigDecimal(4000),
                new BigDecimal(800), true);

        addAvanceAssignmentWithMeasurement(orderLine,
                givenAdvanceType("test2"), new BigDecimal(1000),
                new BigDecimal(600), false);

        assertThat(orderLine.getAdvancePercentage(), equalTo(new BigDecimal(20)
                .divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLine1()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        AdvanceType advanceType1 = AdvanceType.create("test1", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(children.get(0), advanceType1,
                new BigDecimal(1000), new BigDecimal(400), true);

        AdvanceType advanceType2 = AdvanceType.create("test2", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(children.get(1), advanceType2,
                new BigDecimal(2000), new BigDecimal(200), true);

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().getUnitName()
                    .equals("test1")) {
                indirectAdvanceAssignment.setReportGlobalAdvance(true);
            } else {
                indirectAdvanceAssignment.setReportGlobalAdvance(false);
            }
        }

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                40).divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLine2()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        AdvanceType advanceType1 = AdvanceType.create("test1", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(children.get(0), advanceType1,
                new BigDecimal(1000), new BigDecimal(400), true);

        AdvanceType advanceType2 = AdvanceType.create("test2", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(children.get(1), advanceType2,
                new BigDecimal(2000), new BigDecimal(200), true);

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().getUnitName()
                    .equals("test2")) {
                indirectAdvanceAssignment.setReportGlobalAdvance(true);
            } else {
                indirectAdvanceAssignment.setReportGlobalAdvance(false);
            }
        }

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                10).divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLine3()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        AdvanceType advanceType1 = AdvanceType.create("test1", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(children.get(0), advanceType1,
                new BigDecimal(1000), new BigDecimal(400), true);

        AdvanceType advanceType2 = AdvanceType.create("test2", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(children.get(1), advanceType2,
                new BigDecimal(2000), new BigDecimal(200), true);

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                20).divide(new BigDecimal(100)).setScale(2)));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLineSameAdvanceType()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(2000,
                3000);

        List<OrderElement> children = orderElement.getChildren();

        AdvanceType advanceType = PredefinedAdvancedTypes.UNITS.getType();

        addAvanceAssignmentWithMeasurement(children.get(0), advanceType,
                new BigDecimal(1000), new BigDecimal(100), true);

        addAvanceAssignmentWithMeasurement(children.get(1), advanceType,
                new BigDecimal(1000), new BigDecimal(300), true);

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().equals(advanceType)) {
                indirectAdvanceAssignment.setReportGlobalAdvance(true);
            } else {
                indirectAdvanceAssignment.setReportGlobalAdvance(false);
            }
        }

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                20).divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLineSameAdvanceTypeChildren()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(2000,
                3000);

        List<OrderElement> children = orderElement.getChildren();

        AdvanceType advanceType = PredefinedAdvancedTypes.UNITS.getType();

        addAvanceAssignmentWithMeasurement(children.get(0), advanceType,
                new BigDecimal(1000), new BigDecimal(100), true);

        addAvanceAssignmentWithMeasurement(children.get(1), advanceType,
                new BigDecimal(1000), new BigDecimal(300), true);

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().equals(
                    PredefinedAdvancedTypes.CHILDREN.getType())) {
                indirectAdvanceAssignment.setReportGlobalAdvance(true);
            } else {
                indirectAdvanceAssignment.setReportGlobalAdvance(false);
            }
        }

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                22).divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLineWithAssignments1()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderLineGroup orderLineGroup = givenOrderLineGroupWithTwoOrderLines(
                1000,
                2000);

        List<OrderElement> children = orderLineGroup.getChildren();
        addAvanceAssignmentWithMeasurement(children.get(0),
                PredefinedAdvancedTypes.UNITS.getType(), new BigDecimal(1000),
                new BigDecimal(400), true);
        addAvanceAssignmentWithMeasurement(children.get(1),
                PredefinedAdvancedTypes.UNITS.getType(), new BigDecimal(2000),
                new BigDecimal(200), true);

        removeReportGlobalAdvanceFromChildrenAdvance(orderLineGroup);
        addAvanceAssignmentWithMeasurement(orderLineGroup,
                PredefinedAdvancedTypes.PERCENTAGE.getType(), new BigDecimal(
                        100), new BigDecimal(90), true);

        assertThat(orderLineGroup.getAdvancePercentage(), equalTo(new BigDecimal(
                90).divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLineWithAssignments2()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();
        addAvanceAssignmentWithMeasurement(children.get(0),
                PredefinedAdvancedTypes.UNITS.getType(), new BigDecimal(1000),
                new BigDecimal(400), true);
        addAvanceAssignmentWithMeasurement(children.get(1),
                PredefinedAdvancedTypes.UNITS.getType(), new BigDecimal(2000),
                new BigDecimal(200), true);

        addAvanceAssignmentWithMeasurement(orderElement,
                PredefinedAdvancedTypes.PERCENTAGE.getType(), new BigDecimal(
                        100), new BigDecimal(90), false);

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                20).divide(new BigDecimal(100)).setScale(2)));
    }

    @Test
    public void checkAdvanceMeasurementMerge()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        LocalDate one = new LocalDate(2009, 9, 1);
        LocalDate two = new LocalDate(2009, 9, 2);
        LocalDate three = new LocalDate(2009, 9, 3);
        LocalDate four = new LocalDate(2009, 9, 4);
        LocalDate five = new LocalDate(2009, 9, 5);

        AdvanceType advanceType = PredefinedAdvancedTypes.UNITS.getType();

        addAvanceAssignmentWithMeasurements(children.get(0), advanceType, true,
                new BigDecimal(1000), one, new BigDecimal(200), three,
                new BigDecimal(400), five, new BigDecimal(500));

        addAvanceAssignmentWithMeasurements(children.get(1), advanceType, true,
                new BigDecimal(1000), two, new BigDecimal(100), three,
                new BigDecimal(350), four, new BigDecimal(400));

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                43).divide(new BigDecimal(100)).setScale(2)));

        Set<DirectAdvanceAssignment> directAdvanceAssignments = orderElement
                .getDirectAdvanceAssignments();
        assertThat(directAdvanceAssignments.size(), equalTo(0));

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        assertThat(indirectAdvanceAssignments.size(), equalTo(2));

        DirectAdvanceAssignment advanceAssignment = null;
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().equals(advanceType)) {
                advanceAssignment = ((OrderLineGroup) orderElement)
                        .calculateFakeDirectAdvanceAssignment(indirectAdvanceAssignment);
                break;
            }
        }
        assertThat(advanceAssignment.getMaxValue(),
                equalTo(new BigDecimal(2000)));

        SortedSet<AdvanceMeasurement> advanceMeasurements = advanceAssignment
                .getAdvanceMeasurements();
        assertThat(advanceMeasurements.size(), equalTo(5));

        ArrayList<AdvanceMeasurement> list = new ArrayList<AdvanceMeasurement>(
                advanceMeasurements);
        Collections.sort(list, new AdvanceMeasurementComparator());
        Collections.reverse(list);
        Iterator<AdvanceMeasurement> iterator = list.iterator();

        AdvanceMeasurement next = iterator.next();
        assertThat(next.getDate(), equalTo(one));
        assertThat(next.getValue(), equalTo(new BigDecimal(200)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(two));
        assertThat(next.getValue(), equalTo(new BigDecimal(300)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(three));
        assertThat(next.getValue(), equalTo(new BigDecimal(750)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(four));
        assertThat(next.getValue(), equalTo(new BigDecimal(800)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(five));
        assertThat(next.getValue(), equalTo(new BigDecimal(900)));

    }

    private static void addAvanceAssignmentWithMeasurements(
            OrderElement orderElement, AdvanceType advanceType,
            boolean reportGlobalAdvance, BigDecimal maxValue, LocalDate date1,
            BigDecimal value1, LocalDate date2, BigDecimal value2,
            LocalDate five, BigDecimal date3)
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        DirectAdvanceAssignment advanceAssignment = givenAdvanceAssigement(
                maxValue, advanceType);
        advanceAssignment.setReportGlobalAdvance(reportGlobalAdvance);

        AdvanceMeasurement advanceMeasurement1 = AdvanceMeasurement.create();
        advanceMeasurement1.setDate(date1);
        advanceMeasurement1.setValue(value1);
        advanceAssignment.getAdvanceMeasurements().add(advanceMeasurement1);
        advanceMeasurement1.setAdvanceAssignment(advanceAssignment);

        AdvanceMeasurement advanceMeasurement2 = AdvanceMeasurement.create();
        advanceMeasurement2.setDate(date2);
        advanceMeasurement2.setValue(value2);
        advanceAssignment.getAdvanceMeasurements().add(advanceMeasurement2);
        advanceMeasurement2.setAdvanceAssignment(advanceAssignment);

        AdvanceMeasurement advanceMeasurement3 = AdvanceMeasurement.create();
        advanceMeasurement3.setDate(five);
        advanceMeasurement3.setValue(date3);
        advanceAssignment.getAdvanceMeasurements().add(advanceMeasurement3);
        advanceMeasurement3.setAdvanceAssignment(advanceAssignment);

        orderElement.addAdvanceAssignment(advanceAssignment);
    }

    @Test
    public void checkGetAdvanceAssignmentsIdempotenet()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        AdvanceType advanceType = PredefinedAdvancedTypes.UNITS.getType();

        addAvanceAssignmentWithMeasurement(children.get(0), advanceType,
                new BigDecimal(1000), new BigDecimal(200), true);

        addAvanceAssignmentWithMeasurement(children.get(1), advanceType,
                new BigDecimal(2000), new BigDecimal(400), true);

        Set<DirectAdvanceAssignment> directAdvanceAssignments = orderElement
                .getDirectAdvanceAssignments();
        assertThat(directAdvanceAssignments.size(), equalTo(0));

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        assertThat(indirectAdvanceAssignments.size(), equalTo(2));

        DirectAdvanceAssignment advanceAssignment = null;
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().equals(advanceType)) {
                advanceAssignment = ((OrderLineGroup) orderElement)
                        .calculateFakeDirectAdvanceAssignment(indirectAdvanceAssignment);
                break;
            }
        }
        assertThat(advanceAssignment.getMaxValue(),
                equalTo(new BigDecimal(3000)));

        assertThat(advanceAssignment.getAdvanceMeasurements().size(),
                equalTo(1));
        assertThat(advanceAssignment.getAdvanceMeasurements().iterator().next()
                .getValue(), equalTo(new BigDecimal(600)));
    }

    @Test
    public void checkAdvanceMeasurementMergeWithDifferentAdvanceTypes()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        LocalDate one = new LocalDate(2009, 9, 1);
        LocalDate two = new LocalDate(2009, 9, 2);
        LocalDate three = new LocalDate(2009, 9, 3);
        LocalDate four = new LocalDate(2009, 9, 4);
        LocalDate five = new LocalDate(2009, 9, 5);

        AdvanceType advanceType1 = AdvanceType.create("test1", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurements(children.get(0), advanceType1,
                true, new BigDecimal(1000), one, new BigDecimal(200), three,
                new BigDecimal(400), five, new BigDecimal(500));

        AdvanceType advanceType2 = AdvanceType.create("test2", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurements(children.get(1), advanceType2,
                true, new BigDecimal(1000), two, new BigDecimal(100), three,
                new BigDecimal(350), four, new BigDecimal(400));

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                43).divide(new BigDecimal(100)).setScale(2)));

        Set<DirectAdvanceAssignment> directAdvanceAssignments = orderElement
                .getDirectAdvanceAssignments();
        assertThat(directAdvanceAssignments.size(), equalTo(0));

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        assertThat(indirectAdvanceAssignments.size(), equalTo(3));

        DirectAdvanceAssignment advanceAssignment = null;
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().getUnitName()
                    .equals(PredefinedAdvancedTypes.CHILDREN.getTypeName())) {
                advanceAssignment = ((OrderLineGroup) orderElement)
                        .calculateFakeDirectAdvanceAssignment(indirectAdvanceAssignment);
                break;
            }
        }
        assertThat(advanceAssignment.getMaxValue(),
                equalTo(new BigDecimal(100)));

        SortedSet<AdvanceMeasurement> advanceMeasurements = advanceAssignment
                .getAdvanceMeasurements();
        assertThat(advanceMeasurements.size(), equalTo(5));

        ArrayList<AdvanceMeasurement> list = new ArrayList<AdvanceMeasurement>(
                advanceMeasurements);
        Collections.sort(list, new AdvanceMeasurementComparator());
        Collections.reverse(list);
        Iterator<AdvanceMeasurement> iterator = list.iterator();

        AdvanceMeasurement next = iterator.next();
        assertThat(next.getDate(), equalTo(one));
        assertThat(next.getValue(), equalTo(new BigDecimal(6).setScale(2)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(two));
        assertThat(next.getValue(), equalTo(new BigDecimal(13).setScale(2)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(three));
        assertThat(next.getValue(), equalTo(new BigDecimal(36).setScale(2)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(four));
        assertThat(next.getValue(), equalTo(new BigDecimal(40).setScale(2)));

        next = iterator.next();
        assertThat(next.getDate(), equalTo(five));
        assertThat(next.getValue(), equalTo(new BigDecimal(43).setScale(2)));
    }

    @Test
    public void checkGetAdvancePercentageTwoLevelOfDepth1()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderLineGroup orderLineGroup_1 = OrderLineGroup.create();
        orderLineGroup_1.setName("OrderLineGroup 1");
        orderLineGroup_1.setCode("1");

        OrderLineGroup orderLineGroup_1_1 = OrderLineGroup.create();
        orderLineGroup_1_1.setName("OrderLineGroup 1.1");
        orderLineGroup_1_1.setCode("1.1");

        OrderLine orderLine_1_1_1 = givenOrderLine("OrderLine 1.1.1", "1.1.1",
                1000);

        orderLineGroup_1_1.add(orderLine_1_1_1);
        orderLineGroup_1.add(orderLineGroup_1_1);

        AdvanceType advanceType1 = AdvanceType.create("test1", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(orderLine_1_1_1, advanceType1,
                new BigDecimal(10), new BigDecimal(2), true);

        AdvanceType advanceType2 = AdvanceType.create("test2", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        removeReportGlobalAdvanceFromChildrenAdvance(orderLineGroup_1_1);
        addAvanceAssignmentWithMeasurement(orderLineGroup_1_1, advanceType2,
                new BigDecimal(100), new BigDecimal(50), true);

        assertThat(orderLineGroup_1.getDirectAdvanceAssignments().size(),
                equalTo(0));
        assertThat(orderLineGroup_1.getIndirectAdvanceAssignments().size(),
                equalTo(3));
        assertThat(orderLineGroup_1.getAdvancePercentage(),
                equalTo(new BigDecimal(50).setScale(2).divide(
                        new BigDecimal(100))));
    }

    @Test
    public void checkGetAdvancePercentageTwoLevelOfDepth2()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderLineGroup orderLineGroup_1 = OrderLineGroup.create();
        orderLineGroup_1.setName("OrderLineGroup 1");
        orderLineGroup_1.setCode("1");

        OrderLineGroup orderLineGroup_1_1 = OrderLineGroup.create();
        orderLineGroup_1_1.setName("OrderLineGroup 1.1");
        orderLineGroup_1_1.setCode("1.1");

        OrderLine orderLine_1_1_1 = givenOrderLine("OrderLine 1.1.1", "1.1.1",
                1000);

        orderLineGroup_1_1.add(orderLine_1_1_1);
        orderLineGroup_1.add(orderLineGroup_1_1);

        AdvanceType advanceType1 = AdvanceType.create("test1", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(orderLine_1_1_1, advanceType1,
                new BigDecimal(10), new BigDecimal(2), true);

        AdvanceType advanceType2 = AdvanceType.create("test2", new BigDecimal(
                10000), true, new BigDecimal(1), true, false);
        addAvanceAssignmentWithMeasurement(orderLineGroup_1_1, advanceType2,
                new BigDecimal(100), new BigDecimal(50), false);

        assertThat(orderLineGroup_1.getDirectAdvanceAssignments().size(),
                equalTo(0));
        assertThat(orderLineGroup_1.getIndirectAdvanceAssignments().size(),
                equalTo(3));
        assertThat(orderLineGroup_1.getAdvancePercentage(),
                equalTo(new BigDecimal(20).setScale(2).divide(
                        new BigDecimal(100))));
    }

    @Test
    public void checkAdvancePercentageOrderGroupLineWithPercentageAdvanceType()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        AdvanceType advanceType = PredefinedAdvancedTypes.PERCENTAGE.getType();
        addAvanceAssignmentWithMeasurement(children.get(0), advanceType,
                new BigDecimal(100), new BigDecimal(40), true);

        addAvanceAssignmentWithMeasurement(children.get(1), advanceType,
                new BigDecimal(100), new BigDecimal(20), true);

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                26).divide(new BigDecimal(100))));

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().getUnitName()
                    .equals(PredefinedAdvancedTypes.PERCENTAGE.getTypeName())) {
                indirectAdvanceAssignment.setReportGlobalAdvance(true);
            } else {
                indirectAdvanceAssignment.setReportGlobalAdvance(false);
            }
        }

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                26).divide(new BigDecimal(100))));
    }

    @Test
    public void checkAdvanceMeasurementMergePercentageAdvanceType()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(1000,
                2000);

        List<OrderElement> children = orderElement.getChildren();

        LocalDate one = new LocalDate(2009, 9, 1);
        LocalDate two = new LocalDate(2009, 9, 2);
        LocalDate three = new LocalDate(2009, 9, 3);
        LocalDate four = new LocalDate(2009, 9, 4);
        LocalDate five = new LocalDate(2009, 9, 5);

        AdvanceType advanceType = PredefinedAdvancedTypes.PERCENTAGE.getType();

        addAvanceAssignmentWithMeasurements(children.get(0), advanceType, true,
                new BigDecimal(100), two, new BigDecimal(10), three,
                new BigDecimal(20), four, new BigDecimal(40));

        addAvanceAssignmentWithMeasurements(children.get(1), advanceType, true,
                new BigDecimal(100), one, new BigDecimal(10), four,
                new BigDecimal(20), five, new BigDecimal(50));

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                46).divide(new BigDecimal(100)).setScale(2)));

        Set<DirectAdvanceAssignment> directAdvanceAssignments = orderElement
                .getDirectAdvanceAssignments();
        assertThat(directAdvanceAssignments.size(), equalTo(0));

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        assertThat(indirectAdvanceAssignments.size(), equalTo(2));

        DirectAdvanceAssignment advanceAssignment = null;
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().equals(advanceType)) {
                advanceAssignment = ((OrderLineGroup) orderElement)
                        .calculateFakeDirectAdvanceAssignment(indirectAdvanceAssignment);
                break;
            }
        }
        assertThat(advanceAssignment.getMaxValue(),
                equalTo(new BigDecimal(100)));

        SortedSet<AdvanceMeasurement> advanceMeasurements = advanceAssignment
                .getAdvanceMeasurements();
        assertThat(advanceMeasurements.size(), equalTo(5));

        ArrayList<AdvanceMeasurement> list = new ArrayList<AdvanceMeasurement>(
                advanceMeasurements);
        Collections.sort(list, new AdvanceMeasurementComparator());
        Collections.reverse(list);
        Iterator<AdvanceMeasurement> iterator = list.iterator();

        AdvanceMeasurement next = iterator.next();
        assertThat(next.getDate(), equalTo(one));
        assertThat(next.getValue(), equalTo(new BigDecimal(6)));
        // FIXME real value should be: 6.66

        next = iterator.next();
        assertThat(next.getDate(), equalTo(two));
        assertThat(next.getValue(), equalTo(new BigDecimal(9)));
        // FIXME real value should be: 10

        next = iterator.next();
        assertThat(next.getDate(), equalTo(three));
        assertThat(next.getValue(), equalTo(new BigDecimal(12)));
        // FIXME real value should be: 13.33

        next = iterator.next();
        assertThat(next.getDate(), equalTo(four));
        assertThat(next.getValue(), equalTo(new BigDecimal(24)));
        // FIXME real value should be: 26.66

        next = iterator.next();
        assertThat(next.getDate(), equalTo(five));
        assertThat(next.getValue(), equalTo(new BigDecimal(44)));
        // FIXME real value should be: 46.66

    }

    @Test
    public void checkCalculateFakeOrderLineGroup()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        OrderElement orderElement = givenOrderLineGroupWithTwoOrderLines(5000,
                1000);

        List<OrderElement> children = orderElement.getChildren();
        AdvanceType advanceType = PredefinedAdvancedTypes.UNITS.getType();
        addAvanceAssignmentWithoutMeasurement(children.get(0), advanceType,
                new BigDecimal(1000), true);

        LocalDate one = new LocalDate(2009, 9, 1);
        LocalDate two = new LocalDate(2009, 9, 2);
        LocalDate three = new LocalDate(2009, 9, 3);

        addAvanceAssignmentWithMeasurements(children.get(1), advanceType, true,
                new BigDecimal(10000), one, new BigDecimal(100), two,
                new BigDecimal(1000), three, new BigDecimal(5000));

        assertThat(orderElement.getAdvancePercentage(), equalTo(new BigDecimal(
                8).divide(new BigDecimal(100)).setScale(2)));

        Set<IndirectAdvanceAssignment> indirectAdvanceAssignments = ((OrderLineGroup) orderElement)
                .getIndirectAdvanceAssignments();
        assertThat(indirectAdvanceAssignments.size(), equalTo(2));

        DirectAdvanceAssignment advanceAssignment = null;
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : indirectAdvanceAssignments) {
            if (indirectAdvanceAssignment.getAdvanceType().equals(advanceType)) {
                advanceAssignment = ((OrderLineGroup) orderElement)
                        .calculateFakeDirectAdvanceAssignment(indirectAdvanceAssignment);
                break;
            }
        }
        assertThat(advanceAssignment.getMaxValue(), equalTo(new BigDecimal(
                11000)));
        assertThat(advanceAssignment.getLastPercentage(),
                equalTo(new BigDecimal(45).divide(new BigDecimal(100))
                        .setScale(2)));
    }

    public static void removeReportGlobalAdvanceFromChildrenAdvance(
            OrderLineGroup orderLineGroup) {
        for (IndirectAdvanceAssignment indirectAdvanceAssignment : orderLineGroup
                .getIndirectAdvanceAssignments()) {
            if (indirectAdvanceAssignment.getAdvanceType().getUnitName()
                    .equals(PredefinedAdvancedTypes.CHILDREN.getTypeName())) {
                indirectAdvanceAssignment.setReportGlobalAdvance(false);
                break;
            }
        }
    }

}
