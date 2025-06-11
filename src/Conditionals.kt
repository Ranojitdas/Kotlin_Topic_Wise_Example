

fun main(){

    val age = 15

    // if-else as statement
    if (age>=18){
        println("Adult")
    }
    else println("Not adult ")

    // if-else statement as expression
    val isAdult = if (age >= 18) true else false
    println(isAdult)

    // if-elseif-else
    val money = 50000;
    if (money>=100000){
        println("rich")
    }
    else if (money>=10000 && money<=50000 ){
        println("Middle class")
    }
    else println("poor")

    val day = 3
    when (day) {
        1 -> println("Monday")
        2 -> println("Tuesday")
        3 -> println("Wednesday")
        4 -> println("Thursday")
        5 -> println("Friday")
        6 -> println("Saturday")
        7 -> println("Sunday")
        else -> "Invalid day number"
    }

}