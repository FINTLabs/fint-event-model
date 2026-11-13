package no.fint.event.model

import no.fint.event.model.testutils.TestDto
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

class SpringBootCompatibilitySpec extends Specification {

    @AutoCleanup
    def context = new AnnotationConfigApplicationContext(JacksonAutoConfiguration)

    def "Boot mapper preserves flattened event JSON and excludes helper properties"() {
        given:
        def mapper = context.getBean(JsonMapper)
        def event = new Event<TestDto>('rogfk.no', 'employee', 'GET_ALL', 'client')
        event.query = '$filter=name eq \'Ola\''
        event.message = 'Query failed: $filter=name eq \'Ola\''
        event.responseStatus = ResponseStatus.ERROR
        event.statusCode = 'QUERY-1'
        event.problems = [new Problem(field: 'name', message: 'Invalid query', code: '1')]
        event.addData(new TestDto('payload'))

        when:
        def json = EventUtil.toJson(mapper, event)
        def fields = mapper.readValue(json, Map)
        def restored = EventUtil.toEvent(mapper, json)

        then:
        fields.keySet() == ['corrId', 'action', 'operation', 'target', 'status', 'time',
                            'orgId', 'source', 'client', 'data', 'query', 'message',
                            'responseStatus', 'statusCode', 'problems'] as Set
        fields.query == event.query
        fields.message == event.message
        fields.data == [[value: 'payload']]
        fields.problems == [[field: 'name', message: 'Invalid query', code: '1']]
        restored.corrId == event.corrId
        restored.status == Status.NEW
        restored.responseStatus == ResponseStatus.ERROR
        restored.query == event.query
        restored.message == event.message
        restored.statusCode == 'QUERY-1'
        restored.problems[0].code == '1'
        EventUtil.convertEventData(mapper, restored, TestDto)*.value == ['payload']
        EventUtil.convertEventData(mapper, restored, new TypeReference<List<TestDto>>() {})*.value == ['payload']
    }

    def "Boot mapper reads the existing event fixture and ignores future fields"() {
        given:
        def mapper = context.getBean(JsonMapper)
        def fields = mapper.readValue(getClass().getResourceAsStream('/event.json'), Map)
        fields.futureField = 'ignored'

        when:
        def event = EventUtil.toEvent(mapper, mapper.writeValueAsString(fields))

        then:
        event != null
        event.status == Status.NEW
        event.responseStatus == ResponseStatus.ERROR
        event.problems.size() == 2
        event.statusCode == 'JEDI-XX'
    }

    def "Boot mapper keeps masked query helpers out of request JSON"() {
        given:
        def mapper = context.getBean(JsonMapper)
        def request = new EventRequest(query: '$top=5&$filter=name eq \'Ola\'')

        expect:
        request.filteredQuery == '$top=5&$filter=***'
        mapper.readValue(mapper.writeValueAsString(request), Map) == [query: request.query]
    }
}
