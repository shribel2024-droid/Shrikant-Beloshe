package com.example.pharma.domain.model

enum class EquipmentType(val displayName: String, val shortCode: String, val category: String) {
    RMG("Rapid Mixer Granulator (RMG)", "RMG", "Granulation"),
    BLENDER("Blender (Tumble / V-Blender / Bin)", "BLENDER", "Blending"),
    FBP_TOP_SPRAY("Fluid Bed Processor — Top Spray", "FBP-TS", "Drying & Granulation"),
    FBP_WURSTER("Fluid Bed Processor — Bottom Spray / Wurster", "FBP-WURSTER", "Pellet Coating"),
    EXTRUDER("Extruder (Twin/Single Screw)", "EXTRUDER", "Extrusion"),
    SPHERONIZER("Spheronizer", "SPHERONIZER", "Pelletization"),
    PAN_COATER("Pan Coater (Perforated)", "PAN-COATER", "Coating"),
    STIRRER("Stirrer / Liquid Mixer", "STIRRER", "Solution Preparation"),
    ROLLER_COMPACTOR("Roller Compactor", "ROLLER-COMPACTOR", "Dry Granulation"),
    MULTIMILL("Multimill", "MULTIMILL", "Milling"),
    QUADRO_MILL("Quadro Comil", "QUADRO-MILL", "Milling"),
    TABLET_PRESS("Tablet Compression Machine", "TABLET-PRESS", "Compression"),
    CAPSULE_POWDER("Capsule Filler — Powder / Granule", "CAPSULE-POWDER", "Encapsulation"),
    CAPSULE_PELLETS("Capsule Filler — Pellets", "CAPSULE-PELLETS", "Encapsulation")
}

enum class DosageForm(val displayName: String) {
    TABLET_UNCOATED("Tablet — Uncoated"),
    FILM_COATED_TABLET("Film-Coated Tablet"),
    CAPSULE_POWDER("Capsule — Powder/Granule Fill"),
    CAPSULE_PELLET("Capsule — Pellet Fill"),
    PELLETS("Pellets / Multiparticulates")
}

enum class ManufacturingProcess(val displayName: String) {
    DIRECT_COMPRESSION("Direct Compression"),
    DRY_GRANULATION("Dry Granulation — Roller Compaction"),
    WET_GRANULATION("Wet Granulation — High Shear"),
    FLUID_BED_GRANULATION("Fluid Bed Granulation"),
    EXTRUSION_SPHERONIZATION("Extrusion-Spheronization"),
    WURSTER_COATING("Pellet Layering / Coating — Wurster")
}

enum class ValidationStatus {
    VALID,      // Green: Acceptable input/result
    CAUTION,    // Amber: Engineering review recommended
    INVALID     // Red: Outside physical/equipment constraints
}

enum class MethodologyType {
    REFERENCE,          // Directly from validated reference literature / workbook
    STANDARD_PRACTICE,  // Standard industrial engineering scale-up model
    CUSTOM              // Custom user-defined exponent or vendor assumption
}

data class FormulaDetail(
    val formulaId: String,
    val title: String,
    val equationText: String,
    val variablesDescription: Map<String, String>,
    val substitutedText: String,
    val calculatedResultText: String,
    val methodologyType: MethodologyType,
    val engineeringNotes: String,
    val referenceText: String = "Pharmaceutical Process Scale-Up (Levin, 3rd Ed.) / Industrial Scale-Up Guidelines"
)

data class ScaleParameter(
    val name: String,
    val unit: String,
    val sourceValue: Double?,
    val target1Value: Double?,
    val target2Value: Double? = null,
    val target3Value: Double? = null,
    val scaleFactor1: Double? = null,
    val scaleFactor2: Double? = null,
    val scaleFactor3: Double? = null,
    val formulaDetail: FormulaDetail? = null,
    val status: ValidationStatus = ValidationStatus.VALID,
    val notes: String = ""
)

data class CalculationWarning(
    val level: ValidationStatus,
    val message: String,
    val parameterName: String = "",
    val recommendation: String = ""
)

data class ScaleUpResult(
    val equipmentType: EquipmentType,
    val methodologyName: String,
    val methodologyType: MethodologyType,
    val parameters: List<ScaleParameter>,
    val warnings: List<CalculationWarning>,
    val assumptions: List<String>,
    val engineeringNotes: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
