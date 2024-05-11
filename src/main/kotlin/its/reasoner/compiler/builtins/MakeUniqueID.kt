package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class MakeUniqueID : BaseBuiltin() {

    override fun getName(): String = "makeUniqueID"

    override fun getArgLength(): Int = 1

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)
        return context?.env?.bind(args?.get(0), NodeFactory.createLiteral(value.toString())) ?: false
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(MakeUniqueID())
        }

        private var value: Long = 0L
            get(): Long = ++field
    }
}