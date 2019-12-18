package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.model.*;

import java.util.Iterator;

public class ItemRelationResolver extends Resolver {

    protected ItemRelationResolver(ProcessLog processLog) {
        super(processLog);
    }

    @Override
    public void process(LandscapeDescription input, LandscapeImpl landscape) {
        input.getItemDescriptions().forEach(serviceDescription -> {
            Item origin = (Item) Items.pick(serviceDescription, landscape.getItems());
            if (!input.isPartial()) {
                processLog.info("Clearing relations of " + origin);
                origin.getRelations().clear(); //delete all relations on full update
            }
        });

        input.getItemDescriptions().forEach(serviceDescription -> {
            Item origin = (Item) Items.pick(serviceDescription, landscape.getItems());
            serviceDescription.getRelations().forEach(relationDescription -> {

                var fqiSource = FullyQualifiedIdentifier.from(relationDescription.getSource());
                var fqiTarget = FullyQualifiedIdentifier.from(relationDescription.getTarget());
                Item source = (Item) Items.find(fqiSource, landscape.getItems()).orElse(null);
                if (source == null) {
                    processLog.warn("Relation source " + relationDescription.getSource() + " not found");
                    return;
                }
                Item target = (Item) Items.find(fqiTarget, landscape.getItems()).orElse(null);

                if (target == null) {
                    processLog.warn("Relation target " + relationDescription.getTarget() + " not found");
                    return;
                }

                Iterator<RelationItem<Item>> iterator = origin.getRelations().iterator();
                Relation existing = null;
                Relation created = new Relation(source, target);
                while (iterator.hasNext()) {
                    existing = (Relation) iterator.next();
                    if (existing.equals(created)) {
                        processLog.info(String.format("Updating relation between %s and %s", existing.getSource(), existing.getTarget()));
                        existing.setDescription(relationDescription.getDescription());
                        existing.setFormat(relationDescription.getFormat());
                        break;
                    }
                    existing = null; //no break: no hit, will be created below
                }

                if (existing == null) {
                    created.setDescription(relationDescription.getDescription());
                    created.setFormat(relationDescription.getFormat());
                    created.setType(relationDescription.getType());

                    origin.getRelations().add(created);
                    if (source == origin)
                        target.getRelations().add(created);
                    else
                        source.getRelations().add(created);

                    processLog.info(
                            String.format("Adding relation %s between %s and %s", created.getType(), created.getSource(), created.getTarget())
                    );
                }
            });
        });
    }
}