import its.model.DomainModel
import its.model.dictionaries.*
import java.lang.NumberFormatException
import java.util.*


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


    /*val input = Prompt(
        "Выберите используемые входные данные:",
        listOf("X + А / B * C + D / K   -   выбран первый + " to 1,
            "X + А / B * C + D / K   -   выбран * " to 2,
            "X * А ^ B + C + D / K   (где A ^ B уже вычислено)  -   выбран *" to 3,
            "А / B * C + D    -   выбран * " to 4,
            "Arr[B + C]   -   выбран [] " to 5,
            "A * (B * C)  -   выбран первый *" to 6,
            "(X + А) [ B + C * D ]  -   выбран второй +" to 7,)
    ).ask()*/

}

fun main(args: Array<String>) {
    run()
}

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