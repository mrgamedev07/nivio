package de.bonndan.nivio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.bonndan.nivio.assessment.Assessable;
import de.bonndan.nivio.assessment.StatusValue;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.StringUtils;

import java.util.*;

public class Group implements GroupItem, Labeled, Assessable {

    /**
     * Default group identifier (items are assigned to this group if no group is given
     */
    public static final String COMMON = "common";
    private final Map<String, Link> links = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>();
    /**
     * Items belonging to this group. Order is important for layouting (until items are ordered there).
     */
    private final Set<Item> items = new LinkedHashSet<>();
    private String identifier;
    private String owner;
    private String description;
    private String contact;
    private String icon;
    private String color;
    private String landscapeIdentifier;

    public Group(String identifier) {
        setIdentifier(identifier);
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = StringUtils.isEmpty(identifier) ? COMMON : identifier;
    }

    @Override
    public FullyQualifiedIdentifier getFullyQualifiedIdentifier() {
        return FullyQualifiedIdentifier.build(landscapeIdentifier, identifier, null);
    }

    @Override
    public String getName() {
        return identifier;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    @Override
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    @Schema(name = "_links")
    public Map<String, Link> getLinks() {
        return links;
    }

    public Set<Item> getItems() {
        return items;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    @Override
    public String getLabel(String key) {
        return labels.get(key);
    }

    @Override
    public void setLabel(String key, String value) {
        labels.put(key, value);
    }

    @Override
    public Set<StatusValue> getAdditionalStatusValues() {
        return StatusValue.fromMapping(indexedByPrefix(Label.status));
    }

    @JsonIgnore
    @Override
    public List<? extends Assessable> getChildren() {
        return new ArrayList<>(getItems());
    }

    public void setLandscape(String landscapeIdentifier) {
        this.landscapeIdentifier = landscapeIdentifier;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return "Group{" +
                "identifier='" + identifier + '\'' +
                '}';
    }
}
