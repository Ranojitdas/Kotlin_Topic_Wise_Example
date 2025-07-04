// fun keyword used to define a function
fun main(){
    println(add(5,3))
    printMessage("hello")
    multiply(5,4)
    SumWithVar(10,50,50,45,558)
}

// fun funcName( para1,para2): ReturnType{
fun add( a: Int,b: Int): Int{
    return a+b
}
fun printMessage(message: String){
    println(message)
}

fun multiply(a: Int,b: Int) = println("a*b = ${a*b}")

// When we don't know how many parameters we needed we use vararg
fun SumWithVar(vararg numbers: Int){
    var result = 0
    for (number in numbers){
        result = result + number
    }
    println("Sum of the numbers are : $result")

}