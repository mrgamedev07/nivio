package de.bonndan.nivio.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class GroupBuilder {
    private Map<String, Link> links = new HashMap<>();
    private String identifier;
    private String landscapeIdentifier;
    private String owner;
    private String description;
    private String contact;
    private String icon;
    private String color;
    private Map<String, String> labels = new HashMap<>();
    private Set<Item> items = new HashSet<>();

    private GroupBuilder() {
    }

    public static GroupBuilder aGroup() {
        return new GroupBuilder();
    }

    public GroupBuilder withLinks(Map<String, Link> links) {
        this.links = links;
        return this;
    }

    public GroupBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public GroupBuilder withLandscapeIdentifier(String landscapeIdentifier) {
        this.landscapeIdentifier = landscapeIdentifier;
        return this;
    }

    public GroupBuilder withOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public GroupBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public GroupBuilder withContact(String contact) {
        this.contact = contact;
        return this;
    }

    public GroupBuilder withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public GroupBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public GroupBuilder withLabels(Map<String, String> labels) {
        this.labels = labels;
        return this;
    }


    public GroupBuilder withItems(Set<Item> items) {
        this.items = items;
        return this;
    }

    public Group build() {
        Group group = new Group(identifier, landscapeIdentifier, owner, description, contact, icon, color, items);
        group.setLinks(links);
        group.getLabels().putAll(labels);
        return group;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    public String getColor() {
        return color;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

}