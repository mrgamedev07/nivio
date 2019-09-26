package de.bonndan.nivio.output.jgraphx;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStyleRegistry;
import com.mxgraph.view.mxStylesheet;
import de.bonndan.nivio.landscape.*;
import de.bonndan.nivio.output.Icon;
import de.bonndan.nivio.output.IconService;
import de.bonndan.nivio.output.LocalServer;
import de.bonndan.nivio.output.jgraphx.dto.HexVertex;
import de.bonndan.nivio.output.jgraphx.dto.RenderedGraph;
import de.bonndan.nivio.output.jgraphx.dto.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static de.bonndan.nivio.landscape.Status.UNKNOWN;
import static de.bonndan.nivio.util.Color.GRAY;

/**
 * This class is responsible for rendering services and groups nicely with bells and whistles.
 *
 * It receives
 */
public class FinalGraph {

    private final int DEFAULT_ICON_SIZE = 50;
    private final IconService iconService;

    private Logger logger = LoggerFactory.getLogger(FinalGraph.class);
    private Map<Service, mxCell> serviceVertexes = new HashMap<>();
    private mxStylesheet stylesheet;
    private mxGraph graph;
    private Map<String, mxCell> groups = new HashMap<>();

    public FinalGraph(IconService iconService) {
        this.iconService = iconService;
    }

    public mxGraph render(AllGroupsGraph allGroupsGraph, Map<String, GroupGraph> subgraphs) {

        //circular image
        mxGraphics2DCanvas.putShape(mxCircularImageShape.NAME, new mxCircularImageShape());

        //curved edges, https://stackoverflow.com/questions/22746439/jgraphx-custom-layoult-curved-edges
        mxGraphics2DCanvas.putShape(CurvedShape.KEY, new CurvedShape());
        mxStyleRegistry.putValue(CurvedEdgeStyle.KEY, new CurvedEdgeStyle());

        graph = new mxGraph();

        graph.setHtmlLabels(true);
        stylesheet = graph.getStylesheet();


        //graph.orderCells(true, virtualNodes.toArray());
        final List<Service> services = new ArrayList<>();

        allGroupsGraph.getLayoutedGroups().forEach((groupName, mxCell) -> {

            Optional<Service> serviceItem = subgraphs.get(groupName).getServiceVertexesWithRelativeOffset().entrySet().stream()
                    .findFirst()
                    .map(entry -> (Service) entry.getKey());
            LandscapeConfig landscapeConfig = serviceItem.map(service -> service.getLandscape().getConfig()).orElse(null);
            mxGeometry groupGeo = mxCell.getGeometry();
            mxCell groupContainer = (mxCell) graph.insertVertex(
                    graph.getDefaultParent(),
                    groupName, groupName,
                    groupGeo.getX(),
                    groupGeo.getY(),
                    groupGeo.getWidth(),
                    groupGeo.getHeight(),
                    getGroupStyle(groupName, getGroupColor(groupName, landscapeConfig))
            );
            groups.put(groupName, groupContainer);

            GroupGraph groupGraph = subgraphs.get(groupName);
            groupGraph.getServiceVertexesWithRelativeOffset().forEach((service, offset) -> {
                serviceVertexes.put(
                        (Service) service,
                        addServiceVertex(offset, groupContainer, service)
                );
                services.add((Service) service);
            });

            resizeContainer(groupContainer);
        });

        addDataFlow(services);
        addProviderEdges(services);
        renderExtras(serviceVertexes);

        return graph;
    }

    public RenderedGraph getRenderedGraph() {

        RenderedGraph renderedGraph = new RenderedGraph();
        serviceVertexes.forEach((service, mxCell) -> renderedGraph.services.add(new HexVertex(service, mxCell, 1000)));
        groups.forEach((groupName, mxCell) -> renderedGraph.groups.add(new HexVertex(groupName, mxCell, 1000)));

        return renderedGraph;
    }


    /**
     * Adds dataflow edges.
     */
    private void addDataFlow(List<Service> services) {

        services.forEach(service -> service.getDataFlow().forEach(df -> {

            if (df.getSource().equals(df.getTarget()))
                return;

            String id = "df_" + service.getIdentifier() + df.getTarget();
            logger.info("Adding dataflow " + id);
            ServiceItem target = ServiceItems.find(FullyQualifiedIdentifier.from(df.getTarget()), services);
            graph.insertEdge(graph.getDefaultParent(), id, df.getFormat(),
                    serviceVertexes.get(service),
                    serviceVertexes.get(target),
                    getDataFlowStyle(service)
            );
        }));
    }


    /**
     * Adds only the edges which cross group boundaries.
     * <p>
     * These edges are added after layouting in order not to influence it.
     *
     * @param services all services
     */
    private void addProviderEdges(List<Service> services) {

        services.forEach(service -> {


            service.getProvidedBy().forEach(provider -> {

                final String astyle = getProviderEdgeStyle(provider);

                graph.insertEdge(
                        graph.getDefaultParent(), null, "",
                        serviceVertexes.get(provider),
                        serviceVertexes.get(service),
                        astyle
                );
            });
        });
    }

    private void resizeContainer(mxCell cell) {
        Object[] childCells = Arrays.stream(graph.getChildCells(cell)).filter(o -> ((mxCell) o).isVertex()).toArray();

        double amount = DEFAULT_ICON_SIZE * 0.8;
        cell.getGeometry().grow(amount);
        Arrays.stream(childCells).forEach(o -> {
            mxCell child = ((mxCell) o);
            if (StringUtils.isEmpty(child.getValue()))
                return;
            mxGeometry childGeo = child.getGeometry();
            childGeo.setX(childGeo.getX() + amount);
            childGeo.setY(childGeo.getY() + amount);
        });
    }

    private mxCell addServiceVertex(mxPoint offset, mxCell parent, ServiceItem serviceItem) {

        String style = getServiceStyle(serviceItem);

        String name = StringUtils.isEmpty(serviceItem.getName()) ? serviceItem.getIdentifier() : serviceItem.getName();
        if (Lifecycle.PLANNED.equals(serviceItem.getLifecycle())) {
            name = name + "\n(planned)";
        }

        return (mxCell) graph.insertVertex(
                parent,
                serviceItem.getFullyQualifiedIdentifier().toString(),
                name,
                offset.getX(), offset.getY(),
                DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE,
                style
        );
    }

    private void renderExtras(Map<Service, mxCell> map) {

        map.entrySet().forEach(entry -> {
            mxCell cell = entry.getValue();
            mxRectangle cellBounds = graph.getCellBounds(cell);

            //sort statuses, pick worst
            Service service = entry.getKey();
            Optional<StatusItem> displayed = service.getStatuses().stream()
                    .filter(item -> !UNKNOWN.equals(item.getStatus()) && !Status.GREEN.equals(item.getStatus()))
                    .min((statusItem, t1) -> {
                        if (statusItem.getStatus().equals(t1.getStatus())) {
                            return statusItem.getLabel().compareToIgnoreCase(t1.getLabel());
                        }
                        return statusItem.getStatus().isHigherThan(t1.getStatus()) ? -1 : 1;
                    });

            //statuses at left
            int statusBoxSize = DEFAULT_ICON_SIZE / 2;
            if (cellBounds == null) {
                logger.warn("Render extras: no cell bounds for {}", service);
                return;
            }

            displayed.ifPresent(statusItem -> {
                cell.setValue(cell.getValue() + "\n(" + statusItem.getLabel() + "!)");
                cell.setStyle(cell.getStyle()
                        + mxConstants.STYLE_STROKECOLOR + "=" + statusItem.getStatus().toString() + ";"
                        + mxConstants.STYLE_STROKEWIDTH + "=" + 4 + ";"
                        + mxConstants.STYLE_IMAGE + "=" + LocalServer.url("/icons/" + statusItem.getStatus().getSymbol() + ".png") + ";"
                );
            });

            //interfaces
            int intfBoxSize = 10;
            double intfOffsetX = cellBounds.getCenterX() - 0.5 * intfBoxSize;
            double intfOffsetY = cellBounds.getCenterY() - 0.5 * intfBoxSize;
            String intfStyle = mxConstants.STYLE_SHAPE + "=" + mxConstants.SHAPE_ELLIPSE + ";"
                    + mxConstants.STYLE_FONTCOLOR + "=black;"
                    + mxConstants.STYLE_LABEL_POSITION + "=right;"
                    + mxConstants.STYLE_ALIGN + "=left;"
                    + mxConstants.STYLE_FILLCOLOR + "=#" + getGroupColor(service) + ";"
                    + mxConstants.STYLE_STROKEWIDTH + "=0;";


            AtomicInteger count = new AtomicInteger(0);
            Stream<InterfaceItem> sorted = service.getInterfaces().stream()
                    .sorted((interfaceItem, t1) -> interfaceItem.getDescription().compareToIgnoreCase(t1.getDescription()));
            sorted.forEach(intf -> {

                double angleInRadians = -1 + count.getAndIncrement() * 0.5;
                double radius = intfBoxSize * 4;
                double x = Math.cos(angleInRadians) * radius;
                double y = Math.sin(angleInRadians) * radius;

                Object v1 = graph.insertVertex(graph.getDefaultParent(), null,
                        intf.getDescription() + " (" + intf.getFormat() + ")",
                        intfOffsetX + x, intfOffsetY + y, intfBoxSize, intfBoxSize,
                        intfStyle
                );

                graph.insertEdge(
                        graph.getDefaultParent(), null, "",
                        cell,
                        v1,
                        mxConstants.STYLE_ENDARROW + "=none;"
                                + mxConstants.STYLE_STROKEWIDTH + "=2;"
                                + mxConstants.STYLE_STROKECOLOR + "=#" + getGroupColor(service) + ";"
                );
            });
        });

    }

    private String getServiceStyle(ServiceItem serviceItem) {
        String style = getBaseStyle((Service) serviceItem) + ";" + "type=" + serviceItem.getType()
                + ";group=" + serviceItem.getGroup() + ";"
                + "strokeColor=" + getGroupColor((Service) serviceItem) + ";";
        if (Lifecycle.PLANNED.equals(serviceItem.getLifecycle())) {
            style = style + mxConstants.STYLE_DASHED + "=1";
        }
        return style;
    }

    private String getBaseStyle(Service service) {
        Icon type = iconService.getIcon(service);
        if (stylesheet.getStyles().containsKey(type.getUrl().toString())) {
            return type.getUrl().toString();
        }

        Hashtable<String, Object> style = new Hashtable<String, Object>();
        style.put(mxConstants.STYLE_SHAPE, mxCircularImageShape.NAME);
        style.put(mxConstants.STYLE_STROKEWIDTH, 3);
        style.put(mxConstants.STYLE_FILLCOLOR, "white");
        style.put(mxConstants.STYLE_IMAGE, type.getUrl());
        style.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP); //decreases space between label and img
        style.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_BOTTOM);
        style.put(mxConstants.STYLE_FONTCOLOR, "black");

        stylesheet.putCellStyle(type.getUrl().toString(), style);

        return type.getUrl().toString();
    }

    private String getDataFlowStyle(Service service) {
        String groupColor = getGroupColor(service);
        String style = mxConstants.STYLE_STROKEWIDTH + "=4;"
                + mxConstants.STYLE_VERTICAL_LABEL_POSITION + "=bottom;"
                + mxConstants.STYLE_SHAPE + "=" + CurvedShape.KEY + ";"
                + mxConstants.STYLE_EDGE + "=" + CurvedEdgeStyle.KEY + ";"
                + mxConstants.STYLE_LABEL_BACKGROUNDCOLOR + "=#" + groupColor + ";"
                + mxConstants.STYLE_FONTCOLOR + "=black;"
                + getStrokeColor(service);

        if (Lifecycle.PLANNED.equals(service.getLifecycle()) || Lifecycle.END_OF_LIFE.equals(service.getLifecycle())) {
            style = style + mxConstants.STYLE_DASHED + "=1";
        }

        return style;
    }

    private String getGroupStyle(String groupName, String groupColor) {

        String lightened = de.bonndan.nivio.util.Color.lighten(groupColor);
        String style = "type=group;groupColor=" + groupColor + ";"
                + "strokeColor=" + groupColor + ";"
                + "strokeWidth=1;"
                + "rounded=1;"
                + mxConstants.STYLE_FILLCOLOR + "=" + lightened + ";"
                + mxConstants.STYLE_VERTICAL_ALIGN + "=" + mxConstants.ALIGN_BOTTOM + ";"
                + mxConstants.STYLE_VERTICAL_LABEL_POSITION + "=" + mxConstants.ALIGN_TOP + ";"
                + mxConstants.STYLE_FONTCOLOR + "=#" + groupColor + ";";
        return style;
    }

    private String getProviderEdgeStyle(Service provider) {
        String style = mxConstants.STYLE_STROKEWIDTH + "=2;"
                + mxConstants.STYLE_ENDARROW + "=oval;"
                + mxConstants.STYLE_STARTARROW + "=false;"
                + getStrokeColor(provider);

        if (Lifecycle.PLANNED.equals(provider.getLifecycle()) || Lifecycle.END_OF_LIFE.equals(provider.getLifecycle())) {
            style = style + mxConstants.STYLE_DASHED + "=1";
        }

        return style;
    }

    private String getStrokeColor(Service serviceItem) {
        Status providerStatus = Status.highestOf(serviceItem.getStatuses());
        if (Status.RED.equals(providerStatus))
            return mxConstants.STYLE_STROKECOLOR + "=red;";
        if (Status.ORANGE.equals(providerStatus))
            return mxConstants.STYLE_STROKECOLOR + "=orange;";

        String groupColor = getGroupColor(serviceItem);
        logger.info("Dataflow stroke color {} for service {} group {}", groupColor, serviceItem.getIdentifier(), serviceItem.getGroup());
        return mxConstants.STYLE_STROKECOLOR + "=#" + groupColor + ";";
    }

    private String getGroupColor(Service service) {
        if (service.getGroup() == null || service.getGroup().startsWith(Groups.COMMON))
            return GRAY;

        return getGroupColor(service.getGroup(), service.getLandscape().getConfig());
    }

    private String getGroupColor(String name, LandscapeConfig config) {

        if (config == null)
            return "333333";

        return config.getGroupConfig(name)
                .map(LandscapeConfig.GroupConfig::getColor)
                .orElse(de.bonndan.nivio.util.Color.nameToRGB(name, "333333"));
    }
}
