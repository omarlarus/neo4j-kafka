package streams

import org.junit.Test
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.event.LabelEntry
import org.neo4j.graphdb.event.PropertyEntry
import streams.events.EntityType
import streams.events.NodeChange
import streams.events.OperationType
import streams.mocks.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StreamsTransactionEventHandlerTest {

    val router : MockStreamsEventRouter = MockStreamsEventRouter()
    val handler : StreamsTransactionEventHandler = StreamsTransactionEventHandler(router)

    @Test
    fun afterCreatedNodes() {

        val createdNodes = mutableListOf<Node>(MockNode(nodeId = 1))
        val txd = MockTransactionData(createdNodes = createdNodes)
        val previous = handler.beforeCommit(txd)
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.created, router.events[0].meta.operation)
        assertNull(router.events[0].payload.before)
        assertNotNull(router.events[0].payload.after)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)

    }

    @Test
    fun afterCreatedNodesWithLabel() {

        val node = MockNode(nodeId = 1, labels = mutableListOf(Label.label("Test")))
        val createdNodes = mutableListOf<Node>(node)

        val labels = mapOf<Long, List<String>>(1L to listOf("Test"))

        val txd = MockTransactionData(createdNodes = createdNodes)
        val previous = handler.beforeCommit(txd)
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.created, router.events[0].meta.operation)
        assertNull(router.events[0].payload.before)
        assertNotNull(router.events[0].payload.after)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)
        val after : NodeChange = router.events[0].payload.after as NodeChange
        assertEquals(listOf("Test"), after.labels)
    }

    @Test
    fun afterCreatedNodesWithProperties() {

        val node = MockNode(nodeId = 1, properties = hashMapOf<String,Any>("name" to "Omar"))
        val createdNodes = mutableListOf<Node>(node)

        val txd = MockTransactionData(createdNodes = createdNodes)
        val previous = handler.beforeCommit(txd)
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.created, router.events[0].meta.operation)
        assertNull(router.events[0].payload.before)
        assertNotNull(router.events[0].payload.after)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)
        val after : NodeChange = router.events[0].payload.after as NodeChange
        assertEquals("Omar",after.properties!!["name"])
    }

    @Test
    fun afterDeletedNodes() {

        val deletedNodes = mutableListOf<Node>(MockNode(nodeId = 1))
        val txd = MockTransactionData(deletedNodes = deletedNodes)
        val previous = handler.beforeCommit(txd)// PreviousTransactionData(nodeProperties = emptyMap(), nodeLabels = emptyMap(), createdPayload = emptyList(), deletedPayload = emptyList())
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.deleted, router.events[0].meta.operation)
        assertNull(router.events[0].payload.after)
        assertNotNull(router.events[0].payload.before)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)

    }

    @Test
    fun afterDeletedNodesWithLabel() {

        val node = MockNode(nodeId = 1)
        val deletedNodes = mutableListOf<Node>(node)

        val labels = mapOf<Long, List<String>>(1L to listOf("Test"))

        val labelsEntries = mutableListOf<LabelEntry>(MockLabelEntry(
                Label.label("Test"),
                node))

        val txd = MockTransactionData(deletedNodes = deletedNodes, removedLabels = labelsEntries )
        val previous = handler.beforeCommit(txd) // PreviousTransactionData(nodeProperties = emptyMap(), nodeLabels = labels, createdPayload = emptyList(), deletedPayload = emptyList())
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.deleted, router.events[0].meta.operation)
        assertNull(router.events[0].payload.after)
        assertNotNull(router.events[0].payload.before)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)
        val before : NodeChange = router.events[0].payload.before as NodeChange
        assertEquals(listOf("Test"), before.labels)
    }

    @Test
    fun afterDeletedNodesWithProperties() {

        val props = hashMapOf<String,Any>("name" to "Omar")
        val node = MockNode(nodeId = 1)
        val deletedNodes = mutableListOf<Node>(node)

        val removedProps = mutableListOf<PropertyEntry<Node>>(MockPropertyEntry<Node>(node, "name", null, "Omar"))

        val txd = MockTransactionData(deletedNodes = deletedNodes, removedNodeProperties = removedProps )
        val previous = handler.beforeCommit(txd)//PreviousTransactionData(nodeProperties = mapOf(1L to props), nodeLabels = emptyMap(), createdPayload = emptyList(), deletedPayload = emptyList())
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.deleted, router.events[0].meta.operation)
        assertNull(router.events[0].payload.after)
        assertNotNull(router.events[0].payload.before)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)
        val before : NodeChange = router.events[0].payload.before as NodeChange
        assertEquals("Omar",before.properties!!["name"])
    }

    @Test
    fun afterUpdateLabelNodes() {

        val updateNodes = mutableListOf<Node>(MockNode(nodeId = 1,labels = mutableListOf(Label.label("PreTest"),Label.label("Test"))))

        val labels = mutableListOf<LabelEntry>(MockLabelEntry(
                Label.label("Test"),
                updateNodes[0]))

        val txd = MockTransactionData(assignedLabels = labels)
        val previous = handler.beforeCommit(txd)
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.updated, router.events[0].meta.operation)
        assertNotNull(router.events[0].payload.after)
        assertNotNull(router.events[0].payload.before)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)

        val before : NodeChange = router.events[0].payload.before as NodeChange
        assertEquals(listOf("PreTest"),before.labels)

        val after : NodeChange = router.events[0].payload.after as NodeChange
        assertEquals(listOf("PreTest","Test"),after.labels)

    }

    @Test
    fun afterUpdatePropertiesNodes() {
        val prevProps = hashMapOf<String,Any>("name" to "Omar")
        val afterProps = hashMapOf<String,Any>("name" to "Andrea")
        val updateNodes = mutableListOf<Node>(MockNode(nodeId = 1,properties = afterProps))


        val txd = MockTransactionData(assignedNodeProperties = mutableListOf<PropertyEntry<Node>>(MockPropertyEntry<Node>(updateNodes[0], "name", "Andrea", "Omar")))

        val previous = handler.beforeCommit(txd)
        handler.afterCommit(txd, previous)

        assertEquals(1, router.events.size)
        assertEquals(OperationType.updated, router.events[0].meta.operation)
        assertNotNull(router.events[0].payload.after)
        assertNotNull(router.events[0].payload.before)
        assertEquals(1, router.events[0].payload.id)
        assertEquals(EntityType.node, router.events[0].payload.type)

        val before : NodeChange = router.events[0].payload.before as NodeChange
        assertEquals(prevProps,before.properties)

        val after : NodeChange = router.events[0].payload.after as NodeChange
        assertEquals(afterProps,after.properties)

    }

    @Test
    fun beforeCommitAddLabel() {
        val labels = mutableListOf<LabelEntry>(MockLabelEntry(
                Label.label("Test"),
                MockNode(nodeId = 1, labels = mutableListOf(Label.label("PreTest"),Label.label("Test")))))

        val txd = MockTransactionData(assignedLabels = labels)
        val previous = handler.beforeCommit(txd)
        assertEquals(0, previous.nodeProperties.size)
        assertEquals(1, previous.nodeLabels.size)
        assertEquals(1, previous.nodeLabels[1]!!.size)
        assertEquals("PreTest", previous.nodeLabels[1]!![0])

        assertEquals(1, previous.updatedPayloads.size)
        assertEquals (1L, previous.updatedPayloads[0].id)
    }

    @Test
    fun beforeCommitRemoveLabel() {
        val labels = mutableListOf<LabelEntry>(MockLabelEntry(
                Label.label("Test"),
                MockNode(nodeId = 1, labels = mutableListOf(Label.label("PreTest")))))

        val txd = MockTransactionData(removedLabels = labels)
        val previous = handler.beforeCommit(txd)
        assertEquals(0, previous.nodeProperties.size)
        assertEquals(1, previous.nodeLabels.size)
        assertEquals(2, previous.nodeLabels[1]!!.size)
        assertEquals("PreTest", previous.nodeLabels[1]!![0])
        assertEquals("Test", previous.nodeLabels[1]!![1])

        assertEquals(1, previous.updatedPayloads.size)
        assertEquals (1L, previous.updatedPayloads[0].id)
    }

    @Test
    fun beforeCommitAddProperty() {
        val props = mutableListOf<PropertyEntry<Node>>()
        val node = MockNode(1)
        props.add(MockPropertyEntry<Node>(node, "p1", "value", null))
        val txd = MockTransactionData(assignedNodeProperties = props)
        val previous = handler.beforeCommit(txd)
        assertEquals(0, previous.nodeProperties.size)

        assertEquals(1, previous.updatedPayloads.size)
        assertEquals (1L, previous.updatedPayloads[0].id)
    }

    @Test
    fun beforeCommitRemoveProperty() {
        val props = mutableListOf<PropertyEntry<Node>>()
        val node = MockNode(nodeId = 1)
        props.add(MockPropertyEntry<Node>(node, "p1", "value0", "value0"))
        val txd = MockTransactionData(removedNodeProperties = props)
        val previous = handler.beforeCommit(txd)
        assertEquals(1, previous.nodeProperties.size)
        assertEquals("value0", previous.nodeProperties[1]!!["p1"])

        assertEquals(1, previous.updatedPayloads.size)
        assertEquals (1L, previous.updatedPayloads[0].id)

    }

    @Test
    fun beforeCommitSetProperty() {
        val props = mutableListOf<PropertyEntry<Node>>()
        val node = MockNode(nodeId = 1)
        props.add(MockPropertyEntry<Node>(node, "p1", "value1", "value0"))
        val txd = MockTransactionData(assignedNodeProperties = props)
        val previous = handler.beforeCommit(txd)
        assertEquals(1, previous.nodeProperties.size)
        assertEquals("value0", previous.nodeProperties[1]!!["p1"])

        assertEquals(1, previous.updatedPayloads.size)
        assertEquals (1L, previous.updatedPayloads[0].id)
    }


    @Test
    fun beforeCommitMultinodes() {
        val props = mutableListOf<PropertyEntry<Node>>()
        val node = MockNode(nodeId = 1)
        props.add(MockPropertyEntry<Node>(node, "p1", "value1", "value0"))

        val labels = mutableListOf<LabelEntry>(MockLabelEntry(
                Label.label("Test"),
                MockNode(nodeId = 2, labels = mutableListOf(Label.label("PreTest"),Label.label("Test")))))

        val txd = MockTransactionData(assignedNodeProperties = props, assignedLabels = labels)
        val previous = handler.beforeCommit(txd)
        assertEquals(1, previous.nodeProperties.size)
        assertEquals("value0", previous.nodeProperties[1]!!["p1"])

        assertEquals(2, previous.updatedPayloads.size)
        assertEquals (1L, previous.updatedPayloads[0].id)
        assertEquals (2L, previous.updatedPayloads[1].id)
    }
}