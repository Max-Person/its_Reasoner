package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.Util
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class CountValues : BaseBuiltin() {

    override fun getName(): String = "countValues"

    override fun getArgLength(): Int = 3

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)
        val values = HashSet<Node>()

        val arg0 = getArg(0, args, context)
        val arg1 = getArg(1, args, context)

        context?.find(arg0, arg1, null)?.asSequence()?.forEach { triple ->
            val currentObject = triple.getObject()

            if (values.all { value -> !value.sameValueAs(currentObject) }) {
                values.add(currentObject)
            }
        }

        return context?.env?.bind(args?.get(2), Util.makeIntNode(values.size)) ?: false
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(CountValues())
        }
    }
}