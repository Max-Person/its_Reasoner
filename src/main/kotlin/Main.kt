import its.model.DomainSolvingModel
import its.model.definition.Domain
import its.model.definition.ObjectDef
import its.model.definition.loqi.DomainLoqiWriter
import its.model.definition.rdf.DomainRDFFiller
import its.model.definition.types.EnumValue
import its.reasoner.LearningSituation
import its.reasoner.nodes.DecisionTreeReasoner._static.solve
import java.io.File


fun run() {
    val dir = "..\\inputs\\input_examples" //путь к папке с данными
    //val dir = "inputs\\"

    //Создать модель домена
    val model = DomainSolvingModel(dir)

    //Создать условие конкретной задачи
    val i = 3
    val situationDomain = model.domain.copy()
    DomainRDFFiller.fillDomain(situationDomain, "${dir}\\_$i\\$i.ttl")

    val situation = LearningSituation(situationDomain)

    //решение задачи - от наиболее краткого ответа до наиболее подробного - выбрать одно из трех
//    val answer = DomainModel.decisionTree.main.getAnswer(situation) //Получить тру/фолс ответ
//    val path = DomainModel.decisionTree.main.getCorrectPath(situation) //Получить посещенные узлы на самом верхнем уровне (без ухода во вложенные ветки) - в порядке вычисления
//    val trace = DomainModel.decisionTree.main.getTrace(situation) //Получить посещенные узлы по всему дереву - в порядке полного вычисления

    //Прорешать задачу - только для задачи вычисления порядка выражений

    fun getUnevaluated(domain: Domain): List<ObjectDef> {
        return situationDomain.objects.filter {
            it.findPropertyDef("state") != null
                    && it.getPropertyValue("state") == EnumValue("state", "unevaluated")
        }
    }

    var unevaluated = getUnevaluated(situationDomain)
    while (unevaluated.isNotEmpty()) {
        situation.decisionTreeVariables.clear()
        for (x in unevaluated) {
            situation.decisionTreeVariables["X"] = x.reference
            model.decisionTree.solve(situation)
        }
        unevaluated = getUnevaluated(situationDomain)
    }

    DomainLoqiWriter.saveDomain(
        situationDomain.apply { subtract(model.domain) },
        File("${dir}\\$i-s.loqi").bufferedWriter()
    )

}

fun main(args: Array<String>) {
    run()
}
