# Pharma OSD Scale-Up Calculator

### Pharmaceutical Oral Solid Dosage (OSD) Equipment Scale-Up & Technology Transfer Suite

A native Android application engineered for **Formulation R&D**, **Technology Transfer**, **Manufacturing Process Engineering**, and **PMO** professionals. The application provides mathematical modeling, dimensional analysis invariants, and ICH Q8/Q9-aligned scale-up calculations from laboratory scale to pilot and commercial scale.

---

## Key Features & Capabilities

### 1. 14 Comprehensive Equipment Scale-Up Modules
1. **Rapid Mixer Granulator (RMG)**:
   - Constant Tip Speed ($N_2 = N_1 \times D_1 / D_2$)
   - Constant Froude Number ($Fr = N^2 D / g \implies N_2 = N_1 \times \sqrt{D_1 / D_2}$)
   - Live fill ratio tracking (30–70% recommended range) and maximum machine RPM validation.
2. **Tumble & Bin Blenders**:
   - Constant Froude Number and Constant Tip Speed
   - Total revolutions conservation ($N_1 \times t_1 = N_2 \times t_2$)
   - Optimal fill ratio verification (50–60%).
3. **Fluid Bed Processor (FBP) — Top Spray**:
   - Superficial air velocity preservation
   - Screen-area scale factor ($SF = D_2^2 / D_1^2$)
   - Airflow, spray rate, atomizing air, and multi-nozzle distribution.
4. **Fluid Bed Processor (FBP) — Bottom Spray / Wurster**:
   - Column cross-sectional area scaling ($SF = \text{Target Area} / \text{Source Area}$)
   - Single-tube to multi-tube Wurster cluster transitions
   - Proportional partition gap scaling and vendor override support.
5. **Screw Extruders**:
   - Volumetric / Cube rule: $Q_2 = Q_1 \times (D_2 / D_1)^3$
   - Heat-transfer limited rule: $Q_2 = Q_1 \times (D_2 / D_1)^{2.5}$
   - Custom exponent rule ($n$).
6. **Spheronizer**:
   - Constant peripheral friction plate tip speed ($N_2 = N_1 \times D_1 / D_2$)
   - Plate surface-area batch loading ($Load_2 = Load_1 \times (D_2 / D_1)^2$).
7. **Perforated Pan Coaters**:
   - Linear pan peripheral speed matching ($N_2 = N_1 \times D_1 / D_2$)
   - Surface-area basis spray rate scaling ($\text{Batch}^{2/3}$) and linear basis.
8. **Stirrer / Liquid Mixers**:
   - Generalized exponent scaling: $N_2 = N_1 \times (D_1 / D_2)^k$
   - $k=1.0$ (Tip speed), $k=2/3$ (Equal power per volume $P/V$), $k=2.0$ (Equal Reynolds), or custom $k$.
9. **Roller Compactors**:
   - Specific Compaction Force ($SCF = \text{Roll Force} / \text{Roll Width}$ in kN/cm)
   - Peripheral speed and throughput-based roll speed scaling
   - Constant screw-to-roll ratio.
10. **Multimill & Conical Screen Mill (Quadro Comil)**:
    - Constant tip speed particle size distribution (PSD) preservation
    - Cylindrical/conical screen shearing area throughput scaling ($Q_2 = Q_1 \times (D_2/D_1)^2$).
11. **Rotary Tablet Compression Machine**:
    - Theoretical output ($\text{Stations} \times \text{RPM} \times 60$)
    - Adjusted effective output accounting for machine efficiency %, turret utilization %, and reject %
    - Compression cycle and batch run time estimation.
12. **Capsule Fillers (Powder & Pellets)**:
    - Speed (cycles/min), station count, dosing disk / pellet cavity fill mass, effective production rate, and batch duration.

---

### 2. Multi-Target Comparison & Formula Transparency
- Source Scale vs Target 1, Target 2, and Target 3.
- Expandable **View Formula** dialog displaying governing equation, variables description, and actual numerical substituted values (e.g. $N_2 = 500 \times \sqrt{170 / 390} = 330.0\text{ RPM}$).
- Traffic light validation system:
  - 🟢 **Green (Valid)**: Within optimal operating ranges.
  - 🟡 **Amber (Caution)**: Outside ideal recommendation (e.g., RMG fill ratio < 30% or > 70%).
  - 🔴 **Red (Invalid)**: Outside equipment limits (e.g., Target RPM exceeding machine max speed).

---

### 3. Integrated Process Map & Plant Machine Library
- Auto-suggests unit operation sequences based on Dosage Form (Uncoated Tablet, Film-Coated Tablet, Capsule Powder, Capsule Pellets, Pellets) and Manufacturing Process (Direct Compression, Wet Granulation, Dry Granulation, Fluid Bed Granulation, Extrusion-Spheronization, Wurster Coating).
- Machine Library pre-loaded with standard models (Diosna, Glatt, L.B. Bohle, Alexanderwerk, Fette, Korsch, Syntegon/Bosch) with custom plant machine addition support.

---

### 4. Technical Transfer Documentation & Offline-First Persistence
- Native Android **PDF Report Generation** with formatted project metadata, parameter comparison tables, formula details, and compliance footer.
- **Excel-Compatible CSV Export** for technical transfer records and validation dossiers.
- 100% Offline-capable Room database (projects, calculations, machine library, calculation history).

---

### 5. Gemini AI Tech-Transfer Assistant
- Multi-turn conversation specialized as a Senior Pharmaceutical Scale-Up Specialist.
- Dynamic model selection:
  - `gemini-3.5-flash` for general queries
  - `gemini-3.1-pro-preview` for complex reasoning & STEM
  - `gemini-3.1-flash-lite-preview` for rapid questions
- Audio **Text-to-Speech (TTS)** using `gemini-3.8-flash-tts` / Android TTS.
- **Multimodal Image Analysis** using `gemini-3.1-pro-preview` for inspecting equipment nameplates, batch sheets, and tablet photos.

---

## Regulatory Disclaimer
*Calculated values are engineering scale-up estimates and must be verified through formulation development studies, equipment capability qualification, process validation, approved SOPs, and vendor recommendations.*
