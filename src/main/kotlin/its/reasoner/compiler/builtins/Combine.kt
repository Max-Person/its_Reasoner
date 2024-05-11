package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class Combine : BaseBuiltin() {

    override fun getName(): String = "combine"

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)

        val vars = args!!.drop(1)
        var str = ""
        vars.forEach {
            str += lex(it, context) + "§"
        }
        str = str.dropLast(1)

        return context?.env?.bind(args[0], NodeFactory.createLiteral(str)) ?: false
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(Combine())
        }
    }
}