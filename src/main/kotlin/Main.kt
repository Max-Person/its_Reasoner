import its.model.DomainModel
import its.model.dictionaries.*
import its.reasoner.LearningSituation
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer
import its.reasoner.util.JenaUtil
import its.reasoner.util.JenaUtil.POAS_PREF
import java.io.File


fun run() {
    val dir = "..\\inputs\\input_examples_expressions\\"

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
    val i = 7
    val situation = LearningSituation("${dir}\\$i.ttl")

    //Прорешать задачу - только для задачи вычисления порядка выражений
    val stateRdfProperty = situation.model.getProperty(POAS_PREF, "state")
    val unevaluatedRdfResource = situation.model.getResource(JenaUtil.genLink(POAS_PREF, "unevaluated"))
    var unevaluated = situation.model.listSubjectsWithProperty(stateRdfProperty, unevaluatedRdfResource).toList()
    while (unevaluated.isNotEmpty()) {
        situation.decisionTreeVariables.clear()
        for (x in unevaluated) {
            situation.decisionTreeVariables.put("X", x.localName)
            DomainModel.decisionTree.main.getAnswer(situation)
        }
        unevaluated = situation.model.listSubjectsWithProperty(stateRdfProperty, unevaluatedRdfResource).toList()
    }


    //решение задачи - от наиболее краткого ответа до наиболее подробного - выбрать одно из трех
//    val answer = DomainModel.decisionTree.main.getAnswer(situation) //Получить тру/фолс ответ
//    val path = DomainModel.decisionTree.main.getCorrectPath(situation) //Получить посещенные узлы на самом верхнем уровне (без ухода во вложенные ветки) - в порядке вычисления
//    val trace = DomainModel.decisionTree.main.getResults(situation) //Получить посещенные узлы по всему дереву - в порядке полного вычисления

    //Записать рдф-результат
    situation.model.setNsPrefix("", POAS_PREF)
    situation.model.remove(DomainModel.domainRDF).write(File("${dir}\\out.ttl").writer(), "TTL")

}

fun main(args: Array<String>) {
    run()
}