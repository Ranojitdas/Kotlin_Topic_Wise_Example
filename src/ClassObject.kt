// Visibility modifiers : Public , Private, Protected , internal
// internal : visible in the same module
// Default Visibility : Public , visible anywhere
// protected : only visible in the same package to subclasses of that class
// private : only visible in the same class

class Person {

    private val name: String = "Ranojit"
    private val age: Int = 0

    // Method
    fun printInfo(){
        println("Name : $name , Age : $age")
    }

}
 // Primary constructor

class Person2(private val name:String = "",private val age:Int = 0){
    init {
        println("Person is created with name : $name and age : $age")
    }
    fun printInfo(){
        println("Name : $name , Age : $age")
    }
}

class Person3(private val name:String,private val age:Int){
    init {
        println("Person is created with name : $name and age : $age")
    }
    fun printInfo(){
        println("Name : $name , Age : $age")
    }
}


fun main(){

}