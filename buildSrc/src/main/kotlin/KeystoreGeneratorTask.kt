// 导入必须放在文件顶部
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64

// 1. 定义数据类，它们现在是这个模块的一部分
@Serializable
private data class CertificateInfo(val commonName: String, val orgUnit: String, val organization: String, val locality: String, val stateProvince: String, val countryCode: String)

@Serializable
private data class KeystoreConfig(val keystoreName: String, val aliasName: String, val storePassword: String, val keyPassword: String, val certificateInfo: CertificateInfo)

abstract class KeystoreGeneratorTask : DefaultTask() {

    @get:InputFile
    abstract val configFile: RegularFileProperty

    // 🌟 关键修改：Keystore 文件名从 configFile 延迟计算得出。
    @get:OutputFile
    val keystoreFile: Provider<File> = configFile.flatMap { file ->
        // 使用 providers.of 来在配置阶段读取配置文件的内容。
        project.providers.provider {
            val jsonParser = Json { ignoreUnknownKeys = true }
            val configText = file.asFile.readText()
            val config = jsonParser.decodeFromString<KeystoreConfig>(configText)

            // 目标：在配置文件 (keystore_info.json) 所在的目录生成 Keystore 文件。
            // 路径 = 配置文件所在的父目录 / KeystoreConfig中定义的 keystoreName
            file.asFile.parentFile.resolve(config.keystoreName)
        }
    }


    @TaskAction
    fun generate() {
        val configFileValue = configFile.asFile.get()
        if (!configFileValue.exists()) {
            throw GradleException("❌ Config file not found at: ${configFileValue.path}. Please check the 'configFile' property setting.")
        }

        // --- 2. 解析 JSON ---
        val jsonParser = Json { ignoreUnknownKeys = true }
        val configText = configFileValue.readText()
        val config = jsonParser.decodeFromString<KeystoreConfig>(configText)
        logger.lifecycle("✅ Config file parsed successfully.")

        // 这里的 keystoreFile.get() 会执行上面定义的延迟计算逻辑，获取最终文件路径
        val outputKeystoreFile = keystoreFile.get()
        logger.lifecycle("Keystore will be generated at: ${outputKeystoreFile.path}")

        // --- 4. 生成或检查 Keystore 文件 ---
        if (!outputKeystoreFile.exists()) {
            logger.lifecycle("Keystore file not found. Generating a new one...")
            generateKeystoreFile(outputKeystoreFile, config)
        } else {
            logger.lifecycle("✅ Keystore file already exists at: ${outputKeystoreFile.path}")
        }

        // --- 5. 计算并打印散列密钥 ---
        try {
            printKeyHashes(outputKeystoreFile, config.aliasName, config.storePassword)
        } catch (e: Exception) {
            logger.error("❌ Failed to calculate key hashes. Possible reasons: wrong password or alias in config.", e)
        }
    }
    /**
     * 封装生成 Keystore 文件的逻辑 (这部分不变)
     */
    private fun generateKeystoreFile(keystoreFile: File, config: KeystoreConfig) {
        val certInfo = config.certificateInfo
        val dname = "CN=${certInfo.commonName}, OU=${certInfo.orgUnit}, O=${certInfo.organization}, L=${certInfo.locality}, ST=${certInfo.stateProvince}, C=${certInfo.countryCode}"
        val keytoolPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool"

        val command = listOf(
            keytoolPath, "-genkeypair", "-v",
            "-keystore", keystoreFile.absolutePath,
            "-alias", config.aliasName,
            "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
            "-dname", dname,
            "-storepass", config.storePassword,
            "-keypass", config.keyPassword
        )

        logger.lifecycle("🚀 Executing keytool command...")
        project.exec { commandLine(command) }.assertNormalExitValue()

        if (keystoreFile.exists()) {
            logger.lifecycle("\n✅ SUCCESS: Keystore file generated at -> ${keystoreFile.absolutePath}")
            logger.lifecycle("!! IMPORTANT: Back up this file and your passwords securely!")
        } else {
            throw GradleException("❌ Keystore generation failed for an unknown reason.")
        }
    }

    /**
     * 计算并打印散列密钥的方法 (这部分不变)
     */
    private fun printKeyHashes(keystoreFile: File, alias: String, storePass: String) {
        logger.lifecycle("\n========================= Certificate Hashes ========================")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keystoreFile.inputStream().use { keyStore.load(it, storePass.toCharArray()) }

        val certificate = keyStore.getCertificate(alias)
            ?: throw GradleException("❌ Certificate with alias '$alias' not found in the keystore. Please check your 'keystore_info.json'.")

        // 计算 SHA-1 散列密钥
        val sha1Digest = MessageDigest.getInstance("SHA-1")
        val sha1Hash = sha1Digest.digest(certificate.encoded)
        val sha1Base64 = Base64.getEncoder().encodeToString(sha1Hash)
        logger.lifecycle("🔑 SHA-1 Hash Key (Base64 for Facebook, etc.):")
        logger.lifecycle(sha1Base64)

        // 计算 SHA-256 指纹
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val sha256Hash = sha256Digest.digest(certificate.encoded)
        val sha256Fingerprint = sha256Hash.joinToString(separator = ":") { "%02X".format(it) }
        logger.lifecycle("\n🔑 SHA-256 Certificate Fingerprint (for Firebase, etc.):")
        logger.lifecycle(sha256Fingerprint)

        logger.lifecycle("===================================================================")
    }
}
// 任务注册的扩展函数 (保持和上一轮修改一致，接受配置块)
fun Project.registerKeystoreGeneratorTask(
    name: String = "generateKeystoreFromJson",
    action: Action<in KeystoreGeneratorTask>
): TaskProvider<KeystoreGeneratorTask> {
    return tasks.register(name, KeystoreGeneratorTask::class.java) {
        group = "Build Setup"
        description = "Generates a JKS keystore in the same directory as the configuration JSON."

        // 1. 设置默认值 (如果未设置)
        if (!configFile.isPresent) {
            val defaultConfigFile = project.layout.projectDirectory.file("keystore_info.json")
            if (defaultConfigFile.asFile.exists()) {
                configFile.set(defaultConfigFile)
            }
        }

        // 2. 将外部传入的配置应用到任务上 (用户可以覆盖默认的 configFile)
        action.execute(this)
    }
}