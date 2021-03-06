/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds JSON values into the generated OpenAPI model using a JSON Patch
 * like "add" operation that also generated intermediate objects as needed.
 * Any existing property is overwritten.
 *
 * <p>This mapper is applied using the contents of {@code openapi.jsonAdd}.
 * This is run after substitutions so it is unaffected by them.
 */
@SmithyInternalApi
public final class OpenApiJsonAdd implements OpenApiMapper {

    private static final Logger LOGGER = Logger.getLogger(OpenApiJsonAdd.class.getName());

    @Override
    public byte getOrder() {
        // This is run after substitutions.
        return 122;
    }

    @Override
    public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
        Map<String, Node> add = context.getConfig().getJsonAdd();

        if (add.isEmpty()) {
            return node;
        }

        ObjectNode result = node;
        for (Map.Entry<String, Node> entry : add.entrySet()) {
            try {
                LOGGER.info(() -> "OpenAPI `jsonAdd`: adding `" + entry.getKey() + "`");
                result = NodePointer.parse(entry.getKey())
                        .addWithIntermediateValues(result, entry.getValue().toNode())
                        .expectObjectNode();
            } catch (IllegalArgumentException e) {
                throw new OpenApiException(e.getMessage(), e);
            }
        }

        return result;
    }
}
