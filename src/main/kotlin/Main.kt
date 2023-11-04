import its.model.DomainModel
import its.model.dictionaries.*
import its.reasoner.LearningSituation
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner._static.getCorrectPath
import java.util.*
import kotlin.system.measureTimeMillis


fun run() {
    val dir = "..\\inputs\\input_examples\\"
    //val dir = "inputs\\"

    //Создать модель домена (в переменную можно не сохранять, она работает как синглтон)
    DomainModel(
        ClassesDictionary(),
        DecisionTreeVarsDictionary(),
        EnumsDictionary(),
        PropertiesDictionary(),
        RelationshipsDictionary(),
        dir, //Путь к папке input_examples_adj
    )

    //Создать условие конкретной задачи
    val i = 5
    val situation = LearningSituation("${dir}_$i\\$i.ttl") //создать условие задачи из файла рдф

    //решение задачи - от наиболее краткого ответа до наиболее подробного - выбрать одно из трех
    val answer = DomainModel.decisionTree.main.getAnswer(situation) //Получить тру/фолс ответ
    println(answer)
//    val path = DomainModel.decisionTree.main.getCorrectPath(situation) //Получить посещенные узлы на самом верхнем уровне (без ухода во вложенные ветки) - в порядке вычисления
//    val trace = DomainModel.decisionTree.main.getTrace(situation) //Получить посещенные узлы по всему дереву - в порядке полного вычисления


    println()

    val n = 5
    if(false){
        val startTime = System.currentTimeMillis()
        for (i in 0 until n) {
            val situation = LearningSituation("${dir}1.ttl")
            DomainModel.decisionTree.main.getCorrectPath(situation)
        }
        println((System.currentTimeMillis() - startTime) / n)
    }

    if(false){
        val time = measureTimeMillis {
            for (i in 0 until n) {
                val situation = LearningSituation("${dir}1.ttl")
                DomainModel.decisionTree.main.getCorrectPath(situation)
            }
        }
        println(time / n)
    }

    /*val exprY = Operator.fromXMLString(Y1)!!
    val resY = exprY.use(QueryReasoner(model))
    println("Y1 = $resY")
    val exprZ = Operator.fromXMLString(findZSimple)!!
    val resZ = exprZ.use(QueryReasoner(model))
    println("Z = $resZ")
    if(false){
        val startTime = System.currentTimeMillis()
        val n = 1000
        for (i in 0 until n) {
            exprY.use(QueryReasoner(model))
        }
        println((System.currentTimeMillis() - startTime) / n)
    }*/

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