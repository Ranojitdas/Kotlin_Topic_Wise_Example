// Exception handling : We use the try catch block

fun main(){

    try {
        val result = 10/0
    } catch (e: Exception){
        println("Exception : ${e.message}")
    }

    try {
        val data ="123a"
        val number = data.toInt()
        println(number)
    } catch (e: Exception){
        println("Exception : ${e.message}")
    } catch (e: ArithmeticException){
        println("Arithmetic Exception : ${e.message}")
    } finally {
        println("It will be always executed")
    }

    // Custom Exception
    class InvalidInputException(message: String) : Exception(message)
    
}

