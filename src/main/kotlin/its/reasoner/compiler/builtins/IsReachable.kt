package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class IsReachable : BaseBuiltin() {

    override fun getName(): String = "isReachable"

    override fun getArgLength(): Int = 4

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)

        val checked = HashSet<Node>()
        val expected = getArg(3, args, context).literalValue.toString().toBoolean()
        return expected == check(args, context, checked)
    }

    /**
     * Выполняет основную проверку на достижимость
     */
    private fun check(args: Array<out Node>?, context: RuleContext?, checked: MutableSet<Node>): Boolean {
        val current = getArg(0, args, context)
        val predicate = getArg(1, args, context)
        val end = getArg(2, args, context)

        if (current.sameValueAs(end)) {
            return true
        }
        if (context?.contains(current, predicate, end) == true) {
            return true
        }

        return context?.find(current, predicate, null)?.asSequence()?.any { triple ->
            val next = triple.getObject()

            if (checked.any { node -> node.sameValueAs(next) }) {
                false
            } else {
                checked.add(next)
                check(arrayOf(next, predicate, end), context, checked)
            }
        } ?: false
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(IsReachable())
        }
    }
}