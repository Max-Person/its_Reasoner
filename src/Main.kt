import its.model.DomainModel
import its.model.dictionaries.*
import java.lang.NumberFormatException
import java.util.*
import its.model.expressions.Operator
import its.reasoner.operators.QueryReasoner
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr


fun run() {
    val dir = "S:\\engRoute\\CompPrehension_MainDir\\input_examples\\"
    //val dir = "inputs\\"

    DomainModel.collect(
        ClassesDictionary(),
        DecisionTreeVarsDictionary(),
        EnumsDictionary(),
        PropertiesDictionary(),
        RelationshipsDictionary(),
    ).initFrom(dir)


    val input = if(false) Prompt(
        "Выберите используемые входные данные:",
        listOf("X + А / B * C + D / K   -   выбран первый + " to 1,
            "X + А / B * C + D / K   -   выбран * " to 2,
            "X * А ^ B + C + D / K   (где A ^ B уже вычислено)  -   выбран *" to 3,
            "А / B * C + D    -   выбран * " to 4,
            "Arr[B + C]   -   выбран [] " to 5,
            "A * (B * C)  -   выбран первый *" to 6,
            "(X + А) [ B + C * D ]  -   выбран второй +" to 7,)
    ).ask()
    else 5

    val model = ModelFactory.createDefaultModel().read(RDFDataMgr.open("${dir}_$input\\$input.owl"), null)
    val exprY = Operator.fromXMLString(findYSimple)!!
//    val resY = exprY.use(QueryReasoner(model))
//    println("Y = $resY")
    val exprZ = Operator.fromXMLString(findZSimple)!!
    val resZ = exprZ.use(QueryReasoner(model))
    println("Z = $resZ")
    if(false){
        val startTime = System.currentTimeMillis()
        val n = 100
        for (i in 0 until n) {
            exprY.use(QueryReasoner(model))
        }
        println((System.currentTimeMillis() - startTime) / n)
    }

}

fun main(args: Array<String>) {
    run()
}

val findYSimple = """
<GetExtreme varName="y" extremeVarName="y_ex">
    <LogicalNot>
        <ExistenceQuantifier varName="y">
            <CheckRelationship>
                <Variable name="y" />
                <Relationship name="isBetween" />
                <Variable name="y_ex" />
                <DecisionTreeVar name="X" />
            </CheckRelationship>
        </ExistenceQuantifier>
    </LogicalNot>
    <LogicalAnd>
        <LogicalAnd>
            <CheckClass>
                <Variable name="y" />
                <Class name="operator" />
            </CheckClass>
            <Compare operator="EQUAL">
                <GetPropertyValue>
                    <Variable name="y" />
                    <Property name="state" />
                </GetPropertyValue>
                <Enum owner="state" value="unevaluated" />
            </Compare>
        </LogicalAnd>
        <CheckRelationship>
            <Variable name="y" />
            <Relationship name="leftOf" />
            <DecisionTreeVar name="X" />
        </CheckRelationship>
    </LogicalAnd>
</GetExtreme>
""".trimIndent()

val findZSimple = """
<GetExtreme varName="z" extremeVarName="z_ex">
    <LogicalNot>
        <ExistenceQuantifier varName="z">
            <CheckRelationship>
                <Variable name="z" />
                <Relationship name="isBetween" />
                <Variable name="z_ex" />
                <DecisionTreeVar name="X" />
            </CheckRelationship>
        </ExistenceQuantifier>
    </LogicalNot>
    <LogicalAnd>
        <LogicalAnd>
            <CheckClass>
                <Variable name="z" />
                <Class name="operator" />
            </CheckClass>
            <Compare operator="EQUAL">
                <GetPropertyValue>
                    <Variable name="z" />
                    <Property name="state" />
                </GetPropertyValue>
                <Enum owner="state" value="unevaluated" />
            </Compare>
        </LogicalAnd>
        <CheckRelationship>
            <Variable name="z" />
            <Relationship name="rightOf" />
            <DecisionTreeVar name="X" />
        </CheckRelationship>
    </LogicalAnd>
</GetExtreme>
""".trimIndent()

val exprStr1 = """
    <GetExtreme extremeVarName="z_ex" varName="z">
        <LogicalNot>
            <ExistenceQuantifier varName="z">
                <ForAllQuantifier varName="z_ex_t">
                    <LogicalAnd>
                        <CheckClass>
                            <Variable name="z_ex_t" />
                            <Class name="token" />
                        </CheckClass>
                        <CheckRelationship>
                            <Variable name="z_ex_t" />
                            <Relationship name="belongsTo" />
                            <Variable name="z_ex" />
                        </CheckRelationship>
                    </LogicalAnd>
                    <ForAllQuantifier varName="z_t">
                        <LogicalAnd>
                            <CheckClass>
                                <Variable name="z_t" />
                                <Class name="token" />
                            </CheckClass>
                            <CheckRelationship>
                                <Variable name="z_t" />
                                <Relationship name="belongsTo" />
                                <Variable name="z" />
                            </CheckRelationship>
                        </LogicalAnd>
                        <ForAllQuantifier varName="x_t">
                            <LogicalAnd>
                                <CheckClass>
                                    <Variable name="x_t" />
                                    <Class name="token" />
                                </CheckClass>
                                <CheckRelationship>
                                    <Variable name="x_t" />
                                    <Relationship name="belongsTo" />
                                    <DecisionTreeVar name="X" />
                                </CheckRelationship>
                            </LogicalAnd>
                            <CheckRelationship>
                                <Variable name="z_t" />
                                <Relationship name="isBetween" />
                                <Variable name="x_t" />
                                <Variable name="z_ex_t" />
                            </CheckRelationship>
                        </ForAllQuantifier>
                    </ForAllQuantifier>
                </ForAllQuantifier>
            </ExistenceQuantifier>
        </LogicalNot>
        <LogicalAnd>
            <LogicalAnd>
                <CheckClass>
                    <Variable name="z" />
                    <Class name="operator" />
                </CheckClass>
                <Compare operator="EQUAL">
                    <GetPropertyValue>
                        <Variable name="z" />
                        <Property name="state" />
                    </GetPropertyValue>
                    <Enum owner="state" value="unevaluated" />
                </Compare>
            </LogicalAnd>
            <ForAllQuantifier varName="z_t">
                <LogicalAnd>
                    <CheckClass>
                        <Variable name="z_t" />
                        <Class name="token" />
                    </CheckClass>
                    <CheckRelationship>
                        <Variable name="z_t" />
                        <Relationship name="belongsTo" />
                        <Variable name="z" />
                    </CheckRelationship>
                </LogicalAnd>
                <ForAllQuantifier varName="x_t">
                    <LogicalAnd>
                        <CheckClass>
                            <Variable name="x_t" />
                            <Class name="token" />
                        </CheckClass>
                        <CheckRelationship>
                            <Variable name="x_t" />
                            <Relationship name="belongsTo" />
                            <DecisionTreeVar name="X" />
                        </CheckRelationship>
                    </LogicalAnd>
                    <CheckRelationship>
                        <Variable name="z_t" />
                        <Relationship name="rightOf" />
                        <Variable name="x_t" />
                    </CheckRelationship>
                </ForAllQuantifier>
            </ForAllQuantifier>
        </LogicalAnd>
    </GetExtreme>
""".trimIndent()

private class Prompt<Info>(val text: String, val options : List<Pair<String, Info>>){
    private val scanner = Scanner(System.`in`)

    private fun getAnswers(): List<Int>{
        print("Ваш ответ: ")
        while(true){
            try{
                val input = scanner.nextLine()
                return if(input.isBlank()) emptyList() else input.split(',').map{ str ->str.toInt()}
            }catch (e: NumberFormatException){
                print("Неверный формат ввода. Введите ответ заново: ")
            }
        }
    }

    fun ask(): Info {
        if(options.size == 1)
            return options[0].second
        println()
        println(text)
        println("(Элемент управления программой: укажите номер варианта для выбора дальнейших действий.)")
        options.forEachIndexed {i, option -> println(" ${i+1}. ${option.first}") }

        var answers = getAnswers()
        while(answers.size != 1 || answers.any { it-1 !in options.indices}){
            println("Укажите одно число для ответа")
            answers = getAnswers()
        }
        val answer = answers.single()
        return options[answer-1].second
    }
}