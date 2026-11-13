package no.fint.event.model

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

/** Regression cases shared by the Java 8 baseline and the Java 25 migration. */
class EventRegressionSpec extends Specification {

    def "Optional response metadata can be read on fresh events"() {
        expect:
        new Event().getStatusCode() == null
        new Event().getProblems() == null
    }

    def "Status code can be set before other response data"() {
        given:
        def event = new Event()

        when:
        event.setStatusCode('QUERY-1')

        then:
        event.statusCode == 'QUERY-1'
        event.message == null
        event.responseStatus == null
    }

    def "Problems can be set before other response data and cleared later"() {
        given:
        def event = new Event()
        def problem = new Problem(field: 'name', message: 'Required', code: '1')

        when:
        event.setProblems([problem])

        then:
        event.problems == [problem]
        event.message == null
        event.responseStatus == null

        when:
        event.setProblems([])

        then:
        event.problems.empty
    }

    def "Setting a query again replaces its previous value"() {
        given:
        def event = new Event()
        event.setQuery('$filter=name eq \'Ola\'')

        when:
        event.setQuery('$top=5')

        then:
        event.query == '$top=5'
        event.request.filteredQuery == '$top=5'
    }

    def "Filtered message is null when a response exists without a message"() {
        given:
        def event = new Event()
        event.setResponseStatus(ResponseStatus.ACCEPTED)

        expect:
        event.filteredMessage == null
        event.message == null
        event.responseStatus == ResponseStatus.ACCEPTED
    }

    def "Default JSON round trip preserves flattened fields and hides helper properties"() {
        given:
        def event = new Event('rogfk.no', 'employee', 'GET_ALL', 'client')
        event.query = '$filter=name eq \'Ola\''
        event.message = 'Query failed: $filter=name eq \'Ola\''
        event.responseStatus = ResponseStatus.ERROR
        event.statusCode = 'QUERY-1'
        event.problems = [new Problem(field: 'name', message: 'Invalid', code: '1')]
        event.addData([value: 'payload'])

        when:
        def json = EventUtil.toJson(event)
        def fields = new JsonSlurper().parseText(json)
        def restored = EventUtil.toEvent(json)

        then:
        fields.keySet() == ['corrId', 'action', 'operation', 'target', 'status', 'time',
                            'orgId', 'source', 'client', 'data', 'query', 'message',
                            'responseStatus', 'statusCode', 'problems'] as Set
        fields.query == event.query
        fields.message == event.message
        fields.data == [[value: 'payload']]
        restored.corrId == event.corrId
        restored.query == event.query
        restored.message == event.message
        restored.statusCode == 'QUERY-1'
        restored.responseStatus == ResponseStatus.ERROR
        restored.problems[0].code == '1'
        restored.data == [[value: 'payload']]
    }

    def "Default mapper accepts future fields in existing event JSON"() {
        given:
        def fields = new JsonSlurper().parse(getClass().getResource('/event.json'))
        fields.futureField = [value: 'ignored']

        when:
        def event = EventUtil.toEvent(JsonOutput.toJson(fields))

        then:
        event != null
        event.corrId == fields.corrId
        event.status == Status.NEW
        event.responseStatus == ResponseStatus.ERROR
        event.statusCode == 'JEDI-XX'
        event.problems.size() == 2
    }
}
