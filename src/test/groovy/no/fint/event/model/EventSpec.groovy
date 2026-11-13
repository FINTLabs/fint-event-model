package no.fint.event.model

import tools.jackson.databind.json.JsonMapper
import groovy.json.JsonSlurper
import spock.lang.Specification

class EventSpec extends Specification {

    def "Creating an empty Event object with default constructor"() {
        when:
        def event = new Event()

        then:
        event.corrId == null
        event.client == null
        event.data.size() == 0
        event.message == null
        event.query == null
        event.status == null
        event.source == null
        event.orgId == null
        event.time == 0
        event.action == null
    }

    def "Creating a Status.NEW Event object"() {
        when:
        def event = new Event('rogfk.no', 'FK1', DefaultActions.HEALTH, 'VFS')

        then:
        event.corrId != null
        event.client == 'VFS'
        event.data.size() == 0
        event.message == null
        event.query == null
        event.status == Status.NEW
        event.source == 'FK1'
        event.orgId == 'rogfk.no'
        event.time > 0
        event.action == DefaultActions.HEALTH.name()
    }

    def "Copy values from old event object into new"() {
        when:
        def originalEvent = new Event('rogfk.no', 'FK1', 'GET_ALL', 'VFS')
        Event<String> newEvent = new Event<>(originalEvent)

        then:
        newEvent == originalEvent
    }

    def "Check if event is health check when action is null"() {
        when:
        def event = new Event()

        then:
        !event.isHealthCheck()
    }

    def "Check if event is health check when action is HEALTH"() {
        when:
        def event = new Event()
        event.setAction(DefaultActions.HEALTH)

        then:
        event.isHealthCheck()
    }

    def "Check if event is register orgId when action is REGISTER_ORG_ID"() {
        when:
        def event = new Event(action: 'REGISTER_ORG_ID')

        then:
        event.isRegisterOrgId()
    }

    def "Add data to an existing event"() {
        given:
        def event = new Event<String>()

        when:
        event.addData('test1')
        event.addObject('test2')

        then:
        event.data[0] == 'test1'
        event.data[1] == 'test2'
    }

    def "Create EventRequest if value is null when setting query"() {
        given:
        def event = new Event()

        when:
        event.setQuery('test-query')
        def query = event.getQuery()

        then:
        query == 'test-query'
    }

    def "Create EventResponse if value is null when setting message"() {
        given:
        def event = new Event()

        when:
        event.setMessage('test-message')
        def message = event.getMessage()

        then:
        message == 'test-message'
    }

    def "Create EventResponse if value is null when setting ResponseStatus"() {
        given:
        def event = new Event()

        when:
        event.setResponseStatus(ResponseStatus.ERROR)
        def responseStatus = event.getResponseStatus()

        then:
        responseStatus == ResponseStatus.ERROR
    }

    def "Safe to call getters on nested attributes when not set"() {
        when:
        def event = new Event();

        then:
        event.getQuery() == null
        event.getMessage() == null
        event.getResponseStatus() == null
    }

    def "Set request attributes on event"() {
        given:
        def event = new Event(action: 'UPDATE_SOMETHING', source: 'Spock', orgId: 'mock.no', client: 'none')

        when:
        event.setQuery("Some Query")

        then:
        event.getQuery()
    }

    def "Set response attributes on event"() {
        given:
        def event = new Event(action: 'UPDATE_SOMETHING', source: 'Spock', orgId: 'mock.no', client: 'none')

        when:
        event.setResponseStatus(ResponseStatus.ACCEPTED)
        event.setMessage("Doubleplus super")
        event.setStatusCode("R2D2")

        then:
        event.getResponseStatus() == ResponseStatus.ACCEPTED
        event.getMessage()
        event.getStatusCode()
    }

    def "Serialize response object to JSON"() {
        given:
        def event = new Event(action: 'UPDATE_SOMETHING', source: 'Spock', orgId: 'mock.no', client: 'none')

        when:
        event.setResponseStatus(ResponseStatus.ACCEPTED)
        event.setMessage("Doubleplus super")
        event.setStatusCode("R2D2")
        event.setProblems([new Problem(field: "monkey", message: "Only chimpanzees allowed", code: 9999)])

        then:
        event.getResponseStatus() == ResponseStatus.ACCEPTED
        event.getMessage()
        event.getStatusCode()
        event.getProblems().size() == 1

        when:
        def jsonSlurper = new JsonSlurper()
        def objectMapper = JsonMapper.builder().build()
        def result = objectMapper.writeValueAsString(event.getResponse())
        println(result)
        def object = jsonSlurper.parseText(result)

        then:
        object
        object.problems
        object.responseStatus == "ACCEPTED"
    }

    def "Event can be serialized"() {
        given:
        def event = new Event(action: 'UPDATE_SOMETHING', source: 'Spock', orgId: 'mock.no', client: 'none', operation: Operation.UPDATE, target: 'SOMETHING')
        event.setResponseStatus(ResponseStatus.ACCEPTED)
        event.setMessage("Doubleplus super")
        event.setStatusCode("R2D2")
        event.setProblems([new Problem(field: "monkey", message: "Only chimpanzees allowed", code: 9999)])

        when:
        def bos = new ByteArrayOutputStream()
        def oos = new ObjectOutputStream(bos)
        oos.writeObject(event)
        oos.close()
        def bytes = bos.toByteArray()

        then:
        bytes.length

        when:
        def bis = new ByteArrayInputStream(bytes)
        def ois = new ObjectInputStream(bis)
        def copy = ois.readObject()

        then:
        copy instanceof Event
        copy.equals(event)
    }

    def "FilteredQuery masks only the \$filter parameter"() {
        given:
        def req = new EventRequest()
        req.setQuery('$top=10&$filter=name eq \'Ola\'&$orderby=name')

        expect:
        req.getFilteredQuery() == '$top=10&$filter=***&$orderby=name'
        req.getQuery()           == '$top=10&$filter=name eq \'Ola\'&$orderby=name'
    }

    def "FilteredQuery masks leading \$filter without prefix"() {
        given:
        def req = new EventRequest()
        req.setQuery('$filter=name eq \'bob\'')

        expect:
        req.getFilteredQuery() == '$filter=***'
        req.getQuery()         == '$filter=name eq \'bob\''
    }

    def "FilteredQuery returns null when query is null"() {
        expect:
        new EventRequest().getFilteredQuery() == null
    }

    def "FilteredMessage masks only the \$filter parameter"() {
        given:
        def event = new Event()
        event.setMessage('\$top=5&\$filter=secret eq \'X\'&\$orderby=id')

        expect:
        event.getFilteredMessage() == '\$top=5&\$filter=***&\$orderby=id'
        event.getMessage()         == '\$top=5&\$filter=secret eq \'X\'&\$orderby=id'
    }

    def "FilteredMessage masks leading \$filter without prefix"() {
        given:
        def event = new Event()
        event.setMessage('\$filter=password eq \'hunter2\'')

        expect:
        event.getFilteredMessage() == '\$filter=***'
        event.getMessage()         == '\$filter=password eq \'hunter2\''
    }

    def "FilteredMessage masks leading \$filter in message"() {
        given:
        def event = new Event()
        event.setMessage('No case found for query: $filter=tittel eq \'bob\'')

        expect:
        event.getFilteredMessage() == 'No case found for query: $filter=***'
        event.getMessage()         == 'No case found for query: $filter=tittel eq \'bob\''
    }

    def "FilteredMessage returns null when message is null"() {
        expect:
        new Event().getFilteredMessage() == null
    }

}
