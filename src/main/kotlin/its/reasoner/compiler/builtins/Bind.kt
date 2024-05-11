package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class Bind : BaseBuiltin() {

    override fun getName(): String = "bind"

    override fun getArgLength(): Int = 2

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)
        return context?.env?.bind(args?.get(1), args?.get(0)) ?: false
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(Bind())
        }
    }
}