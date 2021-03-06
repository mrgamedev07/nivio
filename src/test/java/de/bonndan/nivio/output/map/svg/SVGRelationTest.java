package de.bonndan.nivio.output.map.svg;

import de.bonndan.nivio.model.*;
import j2html.tags.DomContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static de.bonndan.nivio.model.ItemFactory.getTestItem;
import static org.junit.jupiter.api.Assertions.*;

class SVGRelationTest {

    @Test
    @DisplayName("items without groups use proper fqi")
    public void relationContainsBothEnds() {

        Landscape landscape = LandscapeFactory.createForTesting("l1", "l1Landscape").build();

        //both items have no group
        Item foo = getTestItem(Group.COMMON, "foo", landscape);

        Item bar = getTestItem(Group.COMMON, "bar", landscape);

        Relation itemRelationItem = new Relation(foo, bar);
        SVGRelation svgRelation = new SVGRelation(new HexPath(new ArrayList<>()), "aabbee", itemRelationItem);
        DomContent render = svgRelation.render();
        String render1 = render.render();
        assertTrue(render1.contains("l1/common/foo"));
        assertFalse(render1.contains("l1//foo"));
        assertTrue(render1.contains("l1/common/bar"));
        assertFalse(render1.contains("l1//bar"));
    }

}