/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A meta-validator that checks for the removal of an operation or
 * resource binding from a service or resource.
 *
 * <p>A "RemovedOperationBinding" eventId is used when an operation is
 * removed, and a "RemovedResourceBinding" eventId is used when a
 * resource is removed.
 */
public final class RemovedEntityBinding extends AbstractDiffEvaluator {
    private static final String REMOVED_RESOURCE = "RemovedResourceBinding";
    private static final String REMOVED_OPERATION = "RemovedOperationBinding";

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();
        differences.changedShapes(EntityShape.class).forEach(change -> validateOperation(change, events));
        return events;
    }

    private void validateOperation(ChangedShape<EntityShape> change, List<ValidationEvent> events) {
        findRemoved(change.getOldShape().getOperations(), change.getNewShape().getOperations())
                .forEach(removed -> events.add(createRemovedEvent(REMOVED_OPERATION, change.getNewShape(), removed)));

        findRemoved(change.getOldShape().getResources(), change.getNewShape().getResources())
                .forEach(removed -> events.add(createRemovedEvent(REMOVED_RESOURCE, change.getNewShape(), removed)));
    }

    private Set<ShapeId> findRemoved(Set<ShapeId> oldShapes, Set<ShapeId> newShapes) {
        Set<ShapeId> removed = new HashSet<>(oldShapes);
        removed.removeAll(newShapes);
        return removed;
    }

    private ValidationEvent createRemovedEvent(String eventId, EntityShape entity, ShapeId removedShape) {
        String descriptor = eventId.equals(REMOVED_RESOURCE) ? "Resource" : "Operation";
        String message = String.format(
                "%s binding of `%s` was removed from %s shape, `%s`",
                descriptor, removedShape, entity.getType(), entity.getId());
        return ValidationEvent.builder()
                .eventId(eventId)
                .severity(Severity.ERROR)
                .shape(entity)
                .message(message)
                .build();
    }
}
