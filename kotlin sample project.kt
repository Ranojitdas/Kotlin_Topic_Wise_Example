import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.max

/*
 * SINGLE-FILE BANKING MANAGEMENT SYSTEM
 * Kotlin/JVM console application.
 *
 * Features:
 * - Registration and login
 * - Accounts, deposits, withdrawals, transfers
 * - Transaction history and statements
 * - PIN change and profile updates
 * - Admin reports
 * - File-based persistence
 *
 * Demo note: This is an educational sample, not production banking software.
 * Passwords/PINs and storage are simplified for demonstration purposes.
 */

private const val DATA_FILE = "bank-data.txt"
private const val ADMIN_USERNAME = "admin"
private const val ADMIN_PASSWORD = "admin123"
private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class Customer(
    val id: String,
    var name: String,
    var username: String,
    var password: String,
    var phone: String,
    var email: String,
    var active: Boolean = true
)

data class BankAccount(
    val number: String,
    val customerId: String,
    var type: String,
    var balance: Long = 0L, // stored in paise to avoid floating-point money errors
    var active: Boolean = true
)

data class BankTransaction(
    val id: String,
    val timestamp: String,
    val type: String,
    val accountNumber: String,
    val amount: Long,
    val balanceAfter: Long,
    val description: String,
    val relatedAccount: String = ""
)

class BankingSystem {
    private val customers = linkedMapOf<String, Customer>()
    private val accounts = linkedMapOf<String, BankAccount>()
    private val transactions = mutableListOf<BankTransaction>()
    private var currentCustomer: Customer? = null
    private var running = true

    init {
        loadData()
        if (customers.isEmpty()) seedDemoData()
    }

    fun run() {
        println()
        println("======================================================")
        println("             KOTLIN BANKING MANAGEMENT")
        println("======================================================")
        while (running) {
            if (currentCustomer == null) showGuestMenu() else showCustomerMenu()
        }
        saveData()
        println("Thank you for using Kotlin Banking Management.")
    }

    private fun showGuestMenu() {
        println()
        println("1. Register")
        println("2. Login")
        println("3. View demo credentials")
        println("4. Admin login")
        println("0. Exit")
        when (readInt("Choose: ")) {
            1 -> registerCustomer()
            2 -> login()
            3 -> showDemoCredentials()
            4 -> adminLogin()
            0 -> running = false
            else -> println("Invalid option.")
        }
    }

    private fun showCustomerMenu() {
        val customer = currentCustomer ?: return
        println()
        println("--------------- Welcome, ${customer.name} ---------------")
        println("1. Dashboard")
        println("2. My accounts")
        println("3. Open account")
        println("4. Deposit")
        println("5. Withdraw")
        println("6. Transfer")
        println("7. Transaction history")
        println("8. Account statement")
        println("9. Change password")
        println("10. Update profile")
        println("11. Close account")
        println("12. Logout")
        println("0. Exit")
        when (readInt("Choose: ")) {
            1 -> dashboard()
            2 -> listMyAccounts()
            3 -> openAccount()
            4 -> deposit()
            5 -> withdraw()
            6 -> transfer()
            7 -> transactionHistory()
            8 -> accountStatement()
            9 -> changePassword()
            10 -> updateProfile()
            11 -> closeAccount()
            12 -> logout()
            0 -> {
                saveData()
                running = false
            }
            else -> println("Invalid option.")
        }
    }

    private fun registerCustomer() {
        println("\n--- Customer Registration ---")
        val name = readNonEmpty("Full name: ")
        val username = readNonEmpty("Username: ").lowercase()
        if (customers.values.any { it.username.equals(username, true) }) {
            println("That username is already taken.")
            return
        }
        val password = readPassword("Password (minimum 6 characters): ")
        if (password.length < 6) {
            println("Password must have at least 6 characters.")
            return
        }
        val phone = readNonEmpty("Phone: ")
        val email = readNonEmpty("Email: ")
        val customer = Customer(
            id = "C" + UUID.randomUUID().toString().take(8).uppercase(),
            name = name,
            username = username,
            password = password,
            phone = phone,
            email = email
        )
        customers[customer.id] = customer
        currentCustomer = customer
        println("Registration successful. Your customer ID is ${customer.id}.")
        if (accounts.values.none { it.customerId == customer.id }) {
            createAccountFor(customer, "Savings")
        }
        saveData()
    }

    private fun login() {
        println("\n--- Customer Login ---")
        val username = readNonEmpty("Username: ").lowercase()
        val password = readPassword("Password: ")
        val customer = customers.values.firstOrNull {
            it.username.equals(username, true) && it.password == password && it.active
        }
        if (customer == null) {
            println("Invalid credentials or inactive account.")
            return
        }
        currentCustomer = customer
        println("Login successful.")
        dashboard()
    }

    private fun showDemoCredentials() {
        println("\nDemo customer credentials")
        println("Username: demo")
        println("Password: demo123")
        println("Admin username: admin")
        println("Admin password: admin123")
        println("Use these only for this educational sample.")
    }

    private fun adminLogin() {
        println("\n--- Admin Login ---")
        val username = readNonEmpty("Username: ")
        val password = readPassword("Password: ")
        if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
            adminMenu()
        } else {
            println("Invalid admin credentials.")
        }
    }

    private fun adminMenu() {
        var adminRunning = true
        while (adminRunning) {
            println("\n================ ADMIN PANEL ================")
            println("1. System overview")
            println("2. List customers")
            println("3. List all accounts")
            println("4. Search customer")
            println("5. Search account")
            println("6. Recent transactions")
            println("7. Toggle customer status")
            println("8. Export report")
            println("0. Back")
            when (readInt("Choose: ")) {
                1 -> adminOverview()
                2 -> adminListCustomers()
                3 -> adminListAccounts()
                4 -> adminSearchCustomer()
                5 -> adminSearchAccount()
                6 -> adminRecentTransactions()
                7 -> adminToggleCustomer()
                8 -> exportReport()
                0 -> adminRunning = false
                else -> println("Invalid option.")
            }
        }
    }

    private fun dashboard() {
        val customer = currentCustomer ?: return
        val ownAccounts = accounts.values.filter { it.customerId == customer.id && it.active }
        println("\n---------------- Dashboard ----------------")
        println("Customer: ${customer.name}")
        println("Customer ID: ${customer.id}")
        println("Username: ${customer.username}")
        println("Active accounts: ${ownAccounts.size}")
        println("Total balance: ${money(ownAccounts.sumOf { it.balance })}")
        println("Recent activity:")
        val ownNumbers = ownAccounts.map { it.number }.toSet()
        val recent = transactions.filter {
            it.accountNumber in ownNumbers || it.relatedAccount in ownNumbers
        }.takeLast(5).reversed()
        if (recent.isEmpty()) println("  No transactions yet.")
        recent.forEach { printTransaction(it) }
        println("-------------------------------------------")
    }

    private fun listMyAccounts() {
        val customer = currentCustomer ?: return
        val own = accounts.values.filter { it.customerId == customer.id }
        println("\n--- My Accounts ---")
        if (own.isEmpty()) {
            println("No accounts found.")
            return
        }
        own.forEachIndexed { index, account ->
            println("${index + 1}. ${account.number} | ${account.type} | " +
                "${money(account.balance)} | ${if (account.active) "Active" else "Closed"}")
        }
    }

    private fun openAccount() {
        val customer = currentCustomer ?: return
        println("\nAccount type")
        println("1. Savings")
        println("2. Current")
        println("3. Student")
        val type = when (readInt("Choose: ")) {
            1 -> "Savings"
            2 -> "Current"
            3 -> "Student"
            else -> {
                println("Invalid account type.")
                return
            }
        }
        createAccountFor(customer, type)
        saveData()
    }

    private fun createAccountFor(customer: Customer, type: String) {
        val number = "10" + (10000000..99999999).random().toString()
        val account = BankAccount(number, customer.id, type)
        accounts[number] = account
        println("$type account created. Account number: $number")
    }

    private fun deposit() {
        val account = chooseOwnAccount("Deposit into which account?")
        if (account == null) return
        val amount = readAmount("Deposit amount: ")
        if (amount <= 0) {
            println("Amount must be greater than zero.")
            return
        }
        account.balance += amount
        addTransaction("DEPOSIT", account, amount, "Cash deposit")
        saveData()
        println("Deposit successful. New balance: ${money(account.balance)}")
    }

    private fun withdraw() {
        val account = chooseOwnAccount("Withdraw from which account?")
        if (account == null) return
        val amount = readAmount("Withdrawal amount: ")
        if (amount <= 0) {
            println("Amount must be greater than zero.")
            return
        }
        if (account.balance < amount) {
            println("Insufficient balance.")
            return
        }
        account.balance -= amount
        addTransaction("WITHDRAWAL", account, -amount, "Cash withdrawal")
        saveData()
        println("Withdrawal successful. New balance: ${money(account.balance)}")
    }

    private fun transfer() {
        println("\n--- Transfer Funds ---")
        val source = chooseOwnAccount("Transfer from which account?")
        if (source == null) return
        val destinationNumber = readNonEmpty("Destination account number: ")
        val destination = accounts[destinationNumber]
        if (destination == null || !destination.active) {
            println("Destination account not found or inactive.")
            return
        }
        if (destination.number == source.number) {
            println("Source and destination must be different.")
            return
        }
        val amount = readAmount("Transfer amount: ")
        if (amount <= 0) {
            println("Amount must be greater than zero.")
            return
        }
        if (source.balance < amount) {
            println("Insufficient balance.")
            return
        }
        val note = readLineOptional("Transfer note (optional): ")
        source.balance -= amount
        destination.balance += amount
        addTransaction("TRANSFER_OUT", source, -amount, note.ifBlank { "Funds transferred" }, destination.number)
        addTransaction("TRANSFER_IN", destination, amount, note.ifBlank { "Funds received" }, source.number)
        saveData()
        println("Transfer successful.")
        println("Transferred ${money(amount)} to ${destination.number}.")
        println("Remaining balance: ${money(source.balance)}")
    }

    private fun transactionHistory() {
        val customer = currentCustomer ?: return
        val ownNumbers = accounts.values.filter { it.customerId == customer.id }.map { it.number }.toSet()
        val history = transactions.filter {
            it.accountNumber in ownNumbers || it.relatedAccount in ownNumbers
        }
        println("\n--- Transaction History (${history.size}) ---")
        if (history.isEmpty()) {
            println("No transactions found.")
            return
        }
        history.takeLast(100).reversed().forEach { printTransaction(it) }
    }

    private fun accountStatement() {
        val account = chooseOwnAccount("Generate statement for which account?")
        if (account == null) return
        val history = transactions.filter {
            it.accountNumber == account.number
        }
        println("\n================ ACCOUNT STATEMENT ================")
        println("Account number : ${account.number}")
        println("Account type   : ${account.type}")
        println("Current balance: ${money(account.balance)}")
        println("Generated      : ${now()}")
        println("---------------------------------------------------")
        if (history.isEmpty()) println("No transactions.")
        history.forEach { printTransaction(it) }
        println("===================================================")
        val export = readLineOptional("Save statement to file? (y/n): ")
        if (export.equals("y", true)) {
            val file = File("statement-${account.number}.txt")
            file.writeText(buildString {
                appendLine("ACCOUNT STATEMENT")
                appendLine("Account: ${account.number}")
                appendLine("Type: ${account.type}")
                appendLine("Balance: ${money(account.balance)}")
                appendLine("Generated: ${now()}")
                history.forEach { appendLine(formatTransaction(it)) }
            })
            println("Statement saved to ${file.absolutePath}")
        }
    }

    private fun changePassword() {
        val customer = currentCustomer ?: return
        val old = readPassword("Current password: ")
        if (old != customer.password) {
            println("Current password is incorrect.")
            return
        }
        val newPassword = readPassword("New password: ")
        if (newPassword.length < 6) {
            println("Password must have at least 6 characters.")
            return
        }
        val confirm = readPassword("Confirm new password: ")
        if (newPassword != confirm) {
            println("Passwords do not match.")
            return
        }
        customer.password = newPassword
        saveData()
        println("Password updated.")
    }

    private fun updateProfile() {
        val customer = currentCustomer ?: return
        println("\nLeave a field blank to keep its current value.")
        val name = readLineOptional("Name [${customer.name}]: ")
        val phone = readLineOptional("Phone [${customer.phone}]: ")
        val email = readLineOptional("Email [${customer.email}]: ")
        if (name.isNotBlank()) customer.name = name
        if (phone.isNotBlank()) customer.phone = phone
        if (email.isNotBlank()) customer.email = email
        saveData()
        println("Profile updated.")
    }

    private fun closeAccount() {
        val account = chooseOwnAccount("Close which account?")
        if (account == null) return
        if (account.balance != 0L) {
            println("Account must have zero balance before closing.")
            return
        }
        val confirmation = readLineOptional("Type CLOSE to confirm: ")
        if (confirmation != "CLOSE") {
            println("Cancelled.")
            return
        }
        account.active = false
        saveData()
        println("Account closed.")
    }

    private fun logout() {
        currentCustomer = null
        println("Logged out.")
    }

    private fun chooseOwnAccount(prompt: String): BankAccount? {
        val customer = currentCustomer ?: return null
        val own = accounts.values.filter { it.customerId == customer.id && it.active }
        if (own.isEmpty()) {
            println("You have no active accounts.")
            return null
        }
        println("\n$prompt")
        own.forEachIndexed { i, account ->
            println("${i + 1}. ${account.number} (${account.type}) - ${money(account.balance)}")
        }
        val choice = readInt("Account number from list: ")
        if (choice !in 1..own.size) {
            println("Invalid selection.")
            return null
        }
        return own[choice - 1]
    }

    private fun addTransaction(
        type: String,
        account: BankAccount,
        amount: Long,
        description: String,
        relatedAccount: String = ""
    ) {
        transactions.add(
            BankTransaction(
                id = "T" + UUID.randomUUID().toString().take(10).uppercase(),
                timestamp = now(),
                type = type,
                accountNumber = account.number,
                amount = amount,
                balanceAfter = account.balance,
                description = description,
                relatedAccount = relatedAccount
            )
        )
    }

    private fun printTransaction(transaction: BankTransaction) {
        println(formatTransaction(transaction))
    }

    private fun formatTransaction(transaction: BankTransaction): String {
        val sign = if (transaction.amount >= 0) "+" else "-"
        return "${transaction.timestamp} | ${transaction.type.padEnd(12)} | " +
            "$sign${money(kotlin.math.abs(transaction.amount))} | " +
            "Balance ${money(transaction.balanceAfter)} | ${transaction.description}" +
            if (transaction.relatedAccount.isNotBlank()) " | Related: ${transaction.relatedAccount}" else ""
    }

    private fun adminOverview() {
        println("\n--- System Overview ---")
        println("Customers: ${customers.size}")
        println("Active customers: ${customers.values.count { it.active }}")
        println("Accounts: ${accounts.size}")
        println("Active accounts: ${accounts.values.count { it.active }}")
        println("Transactions: ${transactions.size}")
        println("Total deposits held: ${money(accounts.values.filter { it.active }.sumOf { it.balance })}")
        println("Deposits: ${transactions.count { it.type == "DEPOSIT" }}")
        println("Withdrawals: ${transactions.count { it.type == "WITHDRAWAL" }}")
        println("Transfers: ${transactions.count { it.type.startsWith("TRANSFER") } / 2}")
    }

    private fun adminListCustomers() {
        println("\n--- All Customers ---")
        if (customers.isEmpty()) println("No customers.")
        customers.values.forEach {
            println("${it.id} | ${it.name} | ${it.username} | ${it.phone} | " +
                "${it.email} | ${if (it.active) "Active" else "Disabled"}")
        }
    }

    private fun adminListAccounts() {
        println("\n--- All Accounts ---")
        if (accounts.isEmpty()) println("No accounts.")
        accounts.values.forEach {
            val owner = customers[it.customerId]?.name ?: "Unknown"
            println("${it.number} | $owner | ${it.type} | ${money(it.balance)} | " +
                "${if (it.active) "Active" else "Closed"}")
        }
    }

    private fun adminSearchCustomer() {
        val query = readNonEmpty("Search by name, username, ID, or phone: ")
        val results = customers.values.filter {
            it.name.contains(query, true) ||
                it.username.contains(query, true) ||
                it.id.contains(query, true) ||
                it.phone.contains(query, true)
        }
        if (results.isEmpty()) println("No customers matched.")
        results.forEach {
            println("${it.id} | ${it.name} | ${it.username} | ${it.phone} | ${it.email}")
        }
    }

    private fun adminSearchAccount() {
        val number = readNonEmpty("Account number: ")
        val account = accounts[number]
        if (account == null) {
            println("Account not found.")
            return
        }
        println("Account: ${account.number}")
        println("Owner: ${customers[account.customerId]?.name ?: "Unknown"}")
        println("Type: ${account.type}")
        println("Balance: ${money(account.balance)}")
        println("Status: ${if (account.active) "Active" else "Closed"}")
        transactions.filter { it.accountNumber == number }.forEach { printTransaction(it) }
    }

    private fun adminRecentTransactions() {
        println("\n--- Recent Transactions ---")
        transactions.takeLast(50).reversed().forEach { printTransaction(it) }
    }

    private fun adminToggleCustomer() {
        val id = readNonEmpty("Customer ID: ")
        val customer = customers[id]
        if (customer == null) {
            println("Customer not found.")
            return
        }
        customer.active = !customer.active
        saveData()
        println("${customer.name} is now ${if (customer.active) "active" else "disabled"}.")
    }

    private fun exportReport() {
        val file = File("bank-report.txt")
        file.writeText(buildString {
            appendLine("BANK SYSTEM REPORT")
            appendLine("Generated: ${now()}")
            appendLine("Customers: ${customers.size}")
            appendLine("Accounts: ${accounts.size}")
            appendLine("Transactions: ${transactions.size}")
            appendLine("Total balances: ${money(accounts.values.sumOf { it.balance })}")
            appendLine()
            appendLine("CUSTOMERS")
            customers.values.forEach { appendLine("${it.id},${it.name},${it.username},${it.active}") }
            appendLine()
            appendLine("ACCOUNTS")
            accounts.values.forEach { appendLine("${it.number},${it.customerId},${it.type},${money(it.balance)},${it.active}") }
            appendLine()
            appendLine("TRANSACTIONS")
            transactions.forEach { appendLine(formatTransaction(it)) }
        })
        println("Report exported to ${file.absolutePath}")
    }

    private fun seedDemoData() {
        val demo = Customer(
            id = "C-DEMO",
            name = "Demo Customer",
            username = "demo",
            password = "demo123",
            phone = "9000000000",
            email = "demo@example.com"
        )
        val friend = Customer(
            id = "C-FRIEND",
            name = "Sample Recipient",
            username = "recipient",
            password = "recipient123",
            phone = "9111111111",
            email = "recipient@example.com"
        )
        customers[demo.id] = demo
        customers[friend.id] = friend
        accounts["1000000001"] = BankAccount("1000000001", demo.id, "Savings", 2500000)
        accounts["1000000002"] = BankAccount("1000000002", demo.id, "Student", 500000)
        accounts["1000000003"] = BankAccount("1000000003", friend.id, "Savings", 1000000)
        println("Demo data initialized.")
    }

    private fun saveData() {
        val file = File(DATA_FILE)
        file.writeText(buildString {
            appendLine("KOTLIN_BANK_V1")
            customers.values.forEach {
                appendLine(listOf("C", enc(it.id), enc(it.name), enc(it.username), enc(it.password),
                    enc(it.phone), enc(it.email), it.active).joinToString("|"))
            }
            accounts.values.forEach {
                appendLine(listOf("A", enc(it.number), enc(it.customerId), enc(it.type),
                    it.balance, it.active).joinToString("|"))
            }
            transactions.forEach {
                appendLine(listOf("T", enc(it.id), enc(it.timestamp), enc(it.type), enc(it.accountNumber),
                    it.amount, it.balanceAfter, enc(it.description), enc(it.relatedAccount)).joinToString("|"))
            }
        })
    }

    private fun loadData() {
        val file = File(DATA_FILE)
        if (!file.exists()) return
        try {
            val lines = file.readLines()
            if (lines.firstOrNull() != "KOTLIN_BANK_V1") return
            for (line in lines.drop(1)) {
                val p = line.split("|")
                when (p.firstOrNull()) {
                    "C" -> if (p.size >= 8) {
                        val c = Customer(dec(p[1]), dec(p[2]), dec(p[3]), dec(p[4]),
                            dec(p[5]), dec(p[6]), p[7].toBoolean())
                        customers[c.id] = c
                    }
                    "A" -> if (p.size >= 6) {
                        val a = BankAccount(dec(p[1]), dec(p[2]), dec(p[3]),
                            p[4].toLongOrNull() ?: 0L, p[5].toBoolean())
                        accounts[a.number] = a
                    }
                    "T" -> if (p.size >= 9) {
                        transactions.add(BankTransaction(
                            dec(p[1]), dec(p[2]), dec(p[3]), dec(p[4]),
                            p[5].toLongOrNull() ?: 0L, p[6].toLongOrNull() ?: 0L,
                            dec(p[7]), dec(p[8])
                        ))
                    }
                }
            }
        } catch (exception: Exception) {
            println("Could not read saved data: ${exception.message}")
        }
    }

    private fun enc(value: String): String =
        java.util.Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun dec(value: String): String =
        String(java.util.Base64.getDecoder().decode(value), Charsets.UTF_8)

    private fun readInt(prompt: String): Int {
        print(prompt)
        return readLine()?.trim()?.toIntOrNull() ?: -1
    }

    private fun readAmount(prompt: String): Long {
        print(prompt)
        val input = readLine()?.trim()?.replace(",", "") ?: return -1L
        return try {
            val amount = input.toBigDecimal()
            amount.multiply(java.math.BigDecimal(100)).longValueExact()
        } catch (_: Exception) {
            -1L
        }
    }

    private fun readNonEmpty(prompt: String): String {
        while (true) {
            print(prompt)
            val value = readLine()?.trim().orEmpty()
            if (value.isNotEmpty()) return value
            println("This field cannot be empty.")
        }
    }

    private fun readPassword(prompt: String): String {
        print(prompt)
        return readLine()?.trim().orEmpty()
    }

    private fun readLineOptional(prompt: String): String {
        print(prompt)
        return readLine()?.trim().orEmpty()
    }

    private fun money(paise: Long): String {
        val rupees = paise / 100
        val cents = max(0L, kotlin.math.abs(paise % 100))
        val sign = if (paise < 0) "-" else ""
        return "₹$sign$rupees.%02d".format(cents)
    }

    private fun now(): String = LocalDateTime.now().format(dateFormat)
}
nsider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 042: Collections
// Linked maps preserve insertion order and provide convenient keyed lookup.
// Consider how this principle applies to the registration workflow.
// Consider how this principle applies to deposits and withdrawals.
// Consider how this principle applies to transfers and transaction history.
// Suggested exercise: write a test case for this topic.
// Suggested exercise: describe one limitation and a possible improvement.
// Suggested exercise: inspect how the current implementation handles invalid input.
//
//
// STUDY NOTE 043: Null safety

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

data class Customer(
    val id: String,
    var name: String,
    val username: String,
    var password: String,
    var phone: String,
    var email: String,
    var active: Boolean = true
)

data class Account(
    val number: String,
    val ownerId: String,
    val type: String,
    var balance: BigDecimal = BigDecimal.ZERO,
    var active: Boolean = true
)

data class Transaction(
    val id: String,
    val time: String,
    val accountNumber: String,
    val type: String,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val note: String
)

class BankingSystem {
    private val customers = mutableMapOf<String, Customer>()
    private val accounts = mutableMapOf<String, Account>()
    private val transactions = mutableListOf<Transaction>()
    private var loggedIn: Customer? = null
    private var running = true

    private val formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        loadData()

        if (customers.isEmpty()) {
            createDemoData()
        }
    }

    fun start() {
        println("================================")
        println("     KOTLIN BANKING SYSTEM")
        println("================================")

        while (running) {
            if (loggedIn == null) {
                guestMenu()
            } else {
                customerMenu()
            }
        }

        saveData()
        println("Application closed.")
    }

    private fun guestMenu() {
        println()
        println("1. Register")
        println("2. Login")
        println("3. Show demo credentials")
        println("4. Admin panel")
        println("0. Exit")

        when (readInt("Choose option: ")) {
            1 -> register()
            2 -> login()
            3 -> showCredentials()
            4 -> adminLogin()
            0 -> running = false
            else -> println("Invalid choice.")
        }
    }

    private fun customerMenu() {
        println()
        println("Welcome, ${loggedIn?.name}")
        println("1. Dashboard")
        println("2. My accounts")
        println("3. Open account")
        println("4. Deposit")
        println("5. Withdraw")
        println("6. Transfer money")
        println("7. Transaction history")
        println("8. Change password")
        println("9. Update profile")
        println("10. Close account")
        println("11. Logout")
        println("0. Exit")

        when (readInt("Choose option: ")) {
            1 -> dashboard()
            2 -> listAccounts()
            3 -> openAccount()
            4 -> deposit()
            5 -> withdraw()
            6 -> transfer()
            7 -> history()
            8 -> changePassword()
            9 -> updateProfile()
            10 -> closeAccount()
            11 -> logout()
            0 -> running = false
            else -> println("Invalid choice.")
        }
    }

    private fun register() {
        println("\n--- Registration ---")

        val name = readRequired("Full name: ")
        val username = readRequired("Username: ").lowercase()

        if (customers.values.any {
                it.username == username
            }) {
            println("Username already exists.")
            return
        }

        val password = readRequired("Password: ")

        if (password.length < 6) {
            println("Password must contain at least 6 characters.")
            return
        }

        val phone = readRequired("Phone: ")
        val email = readRequired("Email: ")

        val customer = Customer(
            id = newId("C"),
            name = name,
            username = username,
            password = password,
            phone = phone,
            email = email
        )

        customers[customer.id] = customer
        loggedIn = customer

        openAccountFor(customer, "Savings")
        saveData()

        println("Registration successful.")
        println("Customer ID: ${customer.id}")
    }

    private fun login() {
        println("\n--- Login ---")

        val username = readRequired("Username: ").lowercase()
        val password = readRequired("Password: ")

        val customer = customers.values.find {
            it.username == username &&
            it.password == password &&
            it.active
        }

        if (customer == null) {
            println("Invalid credentials.")
            return
        }

        loggedIn = customer
        println("Login successful.")
        dashboard()
    }

    private fun showCredentials() {
        println("Demo customer: demo / demo123")
        println("Demo admin: admin / admin123")
        println("For learning purposes only.")
    }

    private fun dashboard() {
        val customer = loggedIn ?: return

        val ownAccounts = accounts.values.filter {
            it.ownerId == customer.id && it.active
        }

        val total = ownAccounts.fold(BigDecimal.ZERO) {
                sum, account -> sum + account.balance
        }

        println("\n--- Dashboard ---")
        println("Name: ${customer.name}")
        println("Customer ID: ${customer.id}")
        println("Accounts: ${ownAccounts.size}")
        println("Total balance: ${money(total)}")

        val numbers = ownAccounts.map { it.number }.toSet()

        transactions.filter {
            it.accountNumber in numbers
        }.takeLast(5).reversed().forEach {
            printTransaction(it)
        }
    }

    private fun listAccounts() {
        val customer = loggedIn ?: return

        val own = accounts.values.filter {
            it.ownerId == customer.id
        }

        if (own.isEmpty()) {
            println("No accounts found.")
            return
        }

        println("\n--- Your Accounts ---")

        own.forEachIndexed { index, account ->
            println(
                "${index + 1}. ${account.number} | " +
                "${account.type} | ${money(account.balance)} | " +
                if (account.active) "Active" else "Closed"
            )
        }
    }

    private fun openAccount() {
        println("1. Savings")
        println("2. Current")
        println("3. Student")

        val type = when (readInt("Account type: ")) {
            1 -> "Savings"
            2 -> "Current"
            3 -> "Student"
            else -> {
                println("Invalid account type.")
                return
            }
        }

        val customer = loggedIn ?: return
        openAccountFor(customer, type)
        saveData()
    }

    private fun openAccountFor(
        customer: Customer,
        type: String
    ) {
        var number: String

        do {
            number = "10" + (10000000..99999999).random()
        } while (accounts.containsKey(number))

        accounts[number] = Account(
            number = number,
            ownerId = customer.id,
            type = type
        )

        println("Account created successfully.")
        println("Account number: $number")
    }

    private fun selectAccount(
        prompt: String
    ): Account? {
        val customer = loggedIn ?: return null

        val own = accounts.values.filter {
            it.ownerId == customer.id && it.active
        }

        if (own.isEmpty()) {
            println("No active accounts.")
            return null
        }

        println(prompt)

        own.forEachIndexed { index, account ->
            println(
                "${index + 1}. ${account.number} " +
                "(${account.type}) - ${money(account.balance)}"
            )
        }

        val choice = readInt("Select account: ")

        if (choice !in 1..own.size) {
            println("Invalid selection.")
            return null
        }

        return own[choice - 1]
    }

    private fun deposit() {
        val account = selectAccount("Deposit into:")
            ?: return

        val amount = readAmount("Amount to deposit: ")

        if (amount <= BigDecimal.ZERO) {
            println("Amount must be positive.")
            return
        }

        account.balance += amount

        record(
            account,
            "DEPOSIT",
            amount,
            "Cash deposit"
        )

        saveData()

        println("Deposit successful.")
        println("Balance: ${money(account.balance)}")
    }

    private fun withdraw() {
        val account = selectAccount("Withdraw from:")
            ?: return

        val amount = readAmount("Amount to withdraw: ")

        if (amount <= BigDecimal.ZERO) {
            println("Amount must be positive.")
            return
        }

        if (account.balance < amount) {
            println("Insufficient balance.")
            return
        }

        account.balance -= amount

        record(
            account,
            "WITHDRAWAL",
            amount.negate(),
            "Cash withdrawal"
        )

        saveData()

        println("Withdrawal successful.")
        println("Balance: ${money(account.balance)}")
    }

    private fun transfer() {
        val source = selectAccount("Transfer from:")
            ?: return

        val destinationNumber =
            readRequired("Destination account number: ")

        val destination = accounts[destinationNumber]

        if (destination == null || !destination.active) {
            println("Destination account is unavailable.")
            return
        }

        if (source.number == destination.number) {
            println("Cannot transfer to the same account.")
            return
        }

        val amount = readAmount("Transfer amount: ")

        if (amount <= BigDecimal.ZERO) {
            println("Amount must be positive.")
            return
        }

        if (source.balance < amount) {
            println("Insufficient balance.")
            return
        }

        source.balance -= amount
        destination.balance += amount

        record(
            source,
            "TRANSFER_OUT",
            amount.negate(),
            "Transfer to ${destination.number}"
        )

        record(
            destination,
            "TRANSFER_IN",
            amount,
            "Transfer from ${source.number}"
        )

        saveData()

        println("Transfer successful.")
        println("New balance: ${money(source.balance)}")
    }

    private fun history() {
        val customer = loggedIn ?: return

        val numbers = accounts.values
            .filter { it.ownerId == customer.id }
            .map { it.number }
            .toSet()

        val ownTransactions = transactions.filter {
            it.accountNumber in numbers
        }

        if (ownTransactions.isEmpty()) {
            println("No transactions yet.")
            return
        }

        println("\n--- Transaction History ---")

        ownTransactions.reversed().forEach {
            printTransaction(it)
        }
    }

    private fun record(
        account: Account,
        type: String,
        amount: BigDecimal,
        note: String
    ) {
        transactions.add(
            Transaction(
                id = newId("T"),
                time = LocalDateTime.now().format(formatter),
                accountNumber = account.number,
                type = type,
                amount = amount,
                balanceAfter = account.balance,
                note = note
            )
        )
    }

    private fun printTransaction(
        transaction: Transaction
    ) {
        println(
            "${transaction.time} | ${transaction.type} | " +
            "${money(transaction.amount)} | " +
            "Balance: ${money(transaction.balanceAfter)} | " +
            transaction.note
        )
    }

    private fun changePassword() {
        val customer = loggedIn ?: return

        val oldPassword = readRequired("Current password: ")

        if (oldPassword != customer.password) {
            println("Incorrect current password.")
            return
        }

        val newPassword = readRequired("New password: ")

        if (newPassword.length < 6) {
            println("Password is too short.")
            return
        }

        val confirm = readRequired("Confirm password: ")

        if (newPassword != confirm) {
            println("Passwords do not match.")
            return
        }

        customer.password = newPassword
        saveData()

        println("Password changed.")
    }

    private fun updateProfile() {
        val customer = loggedIn ?: return

        println("Leave blank to keep existing values.")

        val name = readOptional("Name [${customer.name}]: ")
        val phone = readOptional("Phone [${customer.phone}]: ")
        val email = readOptional("Email [${customer.email}]: ")

        if (name.isNotBlank()) customer.name = name
        if (phone.isNotBlank()) customer.phone = phone
        if (email.isNotBlank()) customer.email = email

        saveData()
        println("Profile updated.")
    }

    private fun closeAccount() {
        val account = selectAccount("Close which account?")
            ?: return

        if (account.balance != BigDecimal.ZERO) {
            println("Withdraw the remaining balance first.")
            return
        }

        val confirmation = readRequired(
            "Type CLOSE to confirm: "
        )

        if (confirmation != "CLOSE") {
            println("Cancelled.")
            return
        }

        account.active = false
        saveData()

        println("Account closed.")
    }

    private fun logout() {
        loggedIn = null
        println("Logged out.")
    }

    private fun adminLogin() {
        val username = readRequired("Admin username: ")
        val password = readRequired("Admin password: ")

        if (username != "admin" || password != "admin123") {
            println("Invalid admin credentials.")
            return
        }

        adminMenu()
    }

    private fun adminMenu() {
        var adminRunning = true

        while (adminRunning) {
            println("\n--- Admin Panel ---")
            println("1. Overview")
            println("2. All customers")
            println("3. All accounts")
            println("4. Recent transactions")
            println("0. Back")

            when (readInt("Choose: ")) {
                1 -> adminOverview()
                2 -> adminCustomers()
                3 -> adminAccounts()
                4 -> adminTransactions()
                0 -> adminRunning = false
                else -> println("Invalid choice.")
            }
        }
    }

    private fun adminOverview() {
        val total = accounts.values.fold(BigDecimal.ZERO) {
                sum, account -> sum + account.balance
        }

        println("Customers: ${customers.size}")
        println("Accounts: ${accounts.size}")
        println("Transactions: ${transactions.size}")
        println("Total balances: ${money(total)}")
    }

    private fun adminCustomers() {
        customers.values.forEach {
            println(
                "${it.id} | ${it.name} | ${it.username} | " +
                "${it.phone} | ${it.email} | Active=${it.active}"
            )
        }
    }

    private fun adminAccounts() {
        accounts.values.forEach {
            val owner = customers[it.ownerId]?.name ?: "Unknown"

            println(
                "${it.number} | $owner | ${it.type} | " +
                "${money(it.balance)} | Active=${it.active}"
            )
        }
    }

    private fun adminTransactions() {
        transactions.takeLast(50).reversed().forEach {
            printTransaction(it)
        }
    }

    private fun createDemoData() {
        val demo = Customer(
            id = "C-DEMO",
            name = "Demo Customer",
            username = "demo",
            password = "demo123",
            phone = "9000000000",
            email = "demo@example.com"
        )

        val recipient = Customer(
            id = "C-RECIPIENT",
            name = "Sample Recipient",
            username = "recipient",
            password = "recipient123",
            phone = "9111111111",
            email = "recipient@example.com"
        )

        customers[demo.id] = demo
        customers[recipient.id] = recipient

        accounts["1000000001"] = Account(
            "1000000001",
            demo.id,
            "Savings",
            BigDecimal("25000.00")
        )

        accounts["1000000002"] = Account(
            "1000000002",
            demo.id,
            "Student",
            BigDecimal("5000.00")
        )

        accounts["1000000003"] = Account(
            "1000000003",
            recipient.id,
            "Savings",
            BigDecimal("10000.00")
        )
    }

    // ==========================================
    // DATA PERSISTENCE
    // ==========================================

    private val dataFile = File("bank-data.txt")

    private fun saveData() {
        try {
            dataFile.writeText(
                buildString {
                    appendLine("BANKING_SYSTEM_V1")

                    customers.values.forEach { customer ->
                        appendLine(
                            listOf(
                                "C",
                                encode(customer.id),
                                encode(customer.name),
                                encode(customer.username),
                                encode(customer.password),
                                encode(customer.phone),
                                encode(customer.email),
                                customer.active
                            ).joinToString("|")
                        )
                    }

                    accounts.values.forEach { account ->
                        appendLine(
                            listOf(
                                "A",
                                encode(account.number),
                                encode(account.ownerId),
                                encode(account.type),
                                account.balance.toPlainString(),
                                account.active
                            ).joinToString("|")
                        )
                    }

                    transactions.forEach { transaction ->
                        appendLine(
                            listOf(
                                "T",
                                encode(transaction.id),
                                encode(transaction.time),
                                encode(transaction.accountNumber),
                                encode(transaction.type),
                                transaction.amount.toPlainString(),
                                transaction.balanceAfter.toPlainString(),
                                encode(transaction.note)
                            ).joinToString("|")
                        )
                    }
                }
            )
        } catch (exception: Exception) {
            println("Unable to save data: ${exception.message}")
        }
    }

    private fun loadData() {
        if (!dataFile.exists()) return

        try {
            val lines = dataFile.readLines()

            if (lines.firstOrNull() != "BANKING_SYSTEM_V1") {
                println("Unknown data-file format.")
                return
            }

            for (line in lines.drop(1)) {
                val fields = line.split("|")

                when (fields.firstOrNull()) {
                    "C" -> loadCustomer(fields)
                    "A" -> loadAccount(fields)
                    "T" -> loadTransaction(fields)
                }
            }

            println("Saved data loaded successfully.")
        } catch (exception: Exception) {
            println("Unable to load data: ${exception.message}")
        }
    }

    private fun loadCustomer(fields: List<String>) {
        if (fields.size != 8) return

        val customer = Customer(
            id = decode(fields[1]),
            name = decode(fields[2]),
            username = decode(fields[3]),
            password = decode(fields[4]),
            phone = decode(fields[5]),
            email = decode(fields[6]),
            active = fields[7].toBoolean()
        )

        customers[customer.id] = customer
    }

    private fun loadAccount(fields: List<String>) {
        if (fields.size != 6) return

        val account = Account(
            number = decode(fields[1]),
            ownerId = decode(fields[2]),
            type = decode(fields[3]),
            balance = fields[4].toBigDecimalOrNull()
                ?: BigDecimal.ZERO,
            active = fields[5].toBoolean()
        )

        accounts[account.number] = account
    }

    private fun loadTransaction(fields: List<String>) {
        if (fields.size != 8) return

        val transaction = Transaction(
            id = decode(fields[1]),
            time = decode(fields[2]),
            accountNumber = decode(fields[3]),
            type = decode(fields[4]),
            amount = fields[5].toBigDecimalOrNull()
                ?: BigDecimal.ZERO,
            balanceAfter = fields[6].toBigDecimalOrNull()
                ?: BigDecimal.ZERO,
            note = decode(fields[7])
        )

        transactions.add(transaction)
    }

    private fun encode(value: String): String {
        return java.util.Base64.getEncoder()
            .encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decode(value: String): String {
        return String(
            java.util.Base64.getDecoder().decode(value),
            Charsets.UTF_8
        )
    }

    // ==========================================
    // ACCOUNT SEARCH
    // ==========================================

    private fun searchAccount() {
        val query = readRequired("Account number: ")
        val account = accounts[query]

        if (account == null) {
            println("Account not found.")
            return
        }

        val owner = customers[account.ownerId]

        println("\n--- Account Details ---")
        println("Account number: ${account.number}")
        println("Owner: ${owner?.name ?: "Unknown"}")
        println("Type: ${account.type}")
        println("Balance: ${money(account.balance)}")
        println("Active: ${account.active}")
    }

    // ==========================================
    // CUSTOMER SEARCH
    // ==========================================

    private fun searchCustomer() {
        val query = readRequired("Search customer: ")

        val results = customers.values.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.username.contains(query, ignoreCase = true) ||
            it.id.contains(query, ignoreCase = true)
        }

        if (results.isEmpty()) {
            println("No matching customers.")
            return
        }

        results.forEach { customer ->
            println(
                "${customer.id} | ${customer.name} | " +
                "${customer.username} | ${customer.email}"
            )
        }
    }

    // ==========================================
    // ACCOUNT STATEMENT
    // ==========================================

    private fun printStatement() {
        val account = selectAccount("Choose account for statement:")
            ?: return

        println("\n========== ACCOUNT STATEMENT ==========")
        println("Account: ${account.number}")
        println("Type: ${account.type}")
        println("Current balance: ${money(account.balance)}")
        println("---------------------------------------")

        val entries = transactions.filter {
            it.accountNumber == account.number
        }

        if (entries.isEmpty()) {
            println("No transactions available.")
        } else {
            entries.forEach { printTransaction(it) }
        }

        println("=======================================")
    }

    // ==========================================
    // EXPORT TRANSACTION REPORT
    // ==========================================

    private fun exportTransactions() {
        val file = File("transaction-report.txt")

        try {
            file.writeText(
                buildString {
                    appendLine("BANK TRANSACTION REPORT")
                    appendLine("Generated: ${currentTime()}")
                    appendLine("Total records: ${transactions.size}")
                    appendLine("--------------------------------------")

                    transactions.forEach { transaction ->
                        appendLine(
                            "${transaction.id} | " +
                            "${transaction.time} | " +
                            "${transaction.accountNumber} | " +
                            "${transaction.type} | " +
                            "${money(transaction.amount)} | " +
                            "${money(transaction.balanceAfter)}"
                        )
                    }
                }
            )

            println("Report saved to ${file.absolutePath}")
        } catch (exception: Exception) {
            println("Export failed: ${exception.message}")
        }
    }

    // ==========================================
    // INPUT VALIDATION AND FORMATTING
    // ==========================================

    private fun readInt(prompt: String): Int {
        print(prompt)
        return readLine()?.trim()?.toIntOrNull() ?: -1
    }

    private fun readRequired(prompt: String): String {
        while (true) {
            print(prompt)
            val value = readLine()?.trim().orEmpty()

            if (value.isNotEmpty()) {
                return value
            }

            println("This field cannot be empty.")
        }
    }

    private fun readOptional(prompt: String): String {
        print(prompt)
        return readLine()?.trim().orEmpty()
    }

    private fun readAmount(prompt: String): BigDecimal {
        while (true) {
            print(prompt)
            val input = readLine()?.trim().orEmpty()

            try {
                val amount = BigDecimal(input)
                    .setScale(2, RoundingMode.UNNECESSARY)

                if (amount > BigDecimal.ZERO) {
                    return amount
                }

                println("Amount must be greater than zero.")
            } catch (_: Exception) {
                println("Enter a valid amount with at most two decimals.")
            }
        }
    }

    private fun money(amount: BigDecimal): String {
        return "₹" + amount.setScale(
            2,
            RoundingMode.HALF_UP
        ).toPlainString()
    }

    private fun currentTime(): String {
        return LocalDateTime.now().format(formatter)
    }

    private fun newId(prefix: String): String {
        return prefix + UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(10)
            .uppercase()
    }
}

// ==========================================
// APPLICATION ENTRY POINT
// ==========================================

fun main() {
    val application = BankingSystem()
    application.start()
}



fun main() {
    BankingSystem().run()
}

