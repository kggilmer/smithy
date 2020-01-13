// This file defines test cases that serialize lists in XML documents.
//
// AWS/QUERY serialzies errors using the following format:
//
// <ErrorResponse>
//     <Error>
//         <!-- Set to Sender or Fault. Can be ignored -->
//         <Type>Sender</Type>
//         <!-- Identifies the name of the error shape with the namespace stripped. -->
//         <Code>InvalidParameterValue</Code>
//         <!-- The optionally present message property of the error structure. -->
//         <Message>The message contents</Message>
//         <!-- Other properties of the error structure go here -->
//      </Error>
//      <!-- The always present AWS request ID. -->
//      <RequestId>42d59b56-7407-4c4a-be0f-4c88daeea257</RequestId>
// </ErrorResponse>
//
// The wrapping element name is always "ErrorResponse". It always contains a nested
// element named "Error" that is the error structure contents plus other key value
// pairs. "ErrorResponse" always has a sibling element named "RequestId" that can
// be used to identify the request that caused the failure.
//
// See: https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-api-responses.html#sqs-api-error-response-structure

$version: "0.5.0"

namespace aws.protocols.tests.query

use smithy.test#httpResponseTests

/// This operation has three possible return values:
///
/// 1. A successful response in the form of GreetingWithErrorsOutput
/// 2. An InvalidGreeting error.
/// 3. A BadRequest error.
operation GreetingWithErrors() -> GreetingWithErrorsOutput errors [InvalidGreeting, ComplexError]

apply GreetingWithErrors @httpResponseTests([
    {
        id: "QueryGreetingWithErrors",
        description: "Ensures that operations with errors successfully know how to deserialize the successful response",
        protocol: "aws.query",
        code: 200,
        body: """
              <GreetingWithErrorsResponse xmlns="https://example.com/">
                  <GreetingWithErrorsResult>
                      <greeting>Hello</greeting>
                  </GreetingWithErrorsResult>
              </GreetingWithErrorsResponse>
              """,
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            greeting: "Hello"
        }
    }
])

structure GreetingWithErrorsOutput {
    greeting: String,
}

/// This error is thrown when an invalid greeting value is provided.
@error("client")
structure InvalidGreeting {
    Message: String,
}

apply InvalidGreeting @httpResponseTests([
    {
        id: "QueryInvalidGreetingError",
        description: "Parses simple XML errors",
        protocol: "aws.query",
        params: {
            Message: "Hi"
        },
        code: 400,
        headers: {
            "Content-Type": "text/xml"
        },
        body: """
              <ErrorResponse>
                 <Error>
                    <Type>Sender</Type>
                    <Code>InvalidGreeting</Code>
                    <Message>Hi</Message>
                 </Error>
                 <RequestId>foo-id</RequestId>
              </ErrorResponse>
              """,
        bodyMediaType: "application/xml",
    }
])

/// This error is thrown when a request is invalid.
@error("client")
structure ComplexError {
    TopLevel: String,

    Nested: ComplexNestedErrorData,
}

apply ComplexError @httpResponseTests([
    {
        id: "QueryComplexError",
        protocol: "aws.query",
        params: {
            TopLevel: "Top level",
            Nested: {
                Foo: "bar"
            }
        },
        code: 400,
        headers: {
            "Content-Type": "text/xml"
        },
        body: """
              <ErrorResponse>
                 <Error>
                    <Type>Sender</Type>
                    <Code>ComplexError</Code>
                    <Message>Hi</Message>
                    <TopLevel>Top level</TopLevel>
                    <Nested>
                        <Foo>bar</Foo>
                    </Nested>
                 </Error>
                 <RequestId>foo-id</RequestId>
              </ErrorResponse>
              """,
        bodyMediaType: "application/xml",
    }
])

structure ComplexNestedErrorData {
    Foo: String,
}