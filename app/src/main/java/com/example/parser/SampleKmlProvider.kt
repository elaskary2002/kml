package com.example.parser

object SampleKmlProvider {

    data class SampleProject(
        val id: String,
        val titleEn: String,
        val titleAr: String,
        val descriptionEn: String,
        val descriptionAr: String,
        val kmlContent: String
    )

    val samples: List<SampleProject> = listOf(
        SampleProject(
            id = "highway_alignment",
            titleEn = "Highway Alignment & Stationing",
            titleAr = "مسار طريق سريع ونقاط المحطات (Alignment)",
            descriptionEn = "Engineering road centerline with curve geometry, 200m station marks (CH 0+000 to CH 1+600), and ditch offsets",
            descriptionAr = "محور طريق هندسي مع منحنيات ونقاط ستيشن كل 200م وعلامات حدود الطريق ومناسيب الارتفاع",
            kmlContent = """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Highway Route Alignment - Sector 4</name>
    <description>Engineering Centerline and Survey Station Points</description>
    <Folder>
      <name>Road_Centerline</name>
      <Placemark>
        <name>Centerline_CL_401</name>
        <description>Main Road Axis Station 0+000 to 1+600</description>
        <LineString>
          <tessellate>1</tessellate>
          <coordinates>
            31.2350,30.0440,24.5
            31.2372,30.0458,25.2
            31.2398,30.0475,26.8
            31.2430,30.0490,28.4
            31.2465,30.0501,30.1
            31.2505,30.0510,31.7
            31.2548,30.0515,33.0
            31.2592,30.0518,34.2
          </coordinates>
        </LineString>
      </Placemark>
      <Placemark>
        <name>Right_Shoulder</name>
        <description>Right Edge of Pavement (12.0m Offset)</description>
        <LineString>
          <tessellate>1</tessellate>
          <coordinates>
            31.2351,30.0439,24.3
            31.2373,30.0457,25.0
            31.2399,30.0474,26.6
            31.2431,30.0489,28.2
            31.2466,30.0500,29.9
            31.2506,30.0509,31.5
            31.2549,30.0514,32.8
            31.2593,30.0517,34.0
          </coordinates>
        </LineString>
      </Placemark>
      <Placemark>
        <name>Left_Shoulder</name>
        <description>Left Edge of Pavement (12.0m Offset)</description>
        <LineString>
          <tessellate>1</tessellate>
          <coordinates>
            31.2349,30.0441,24.3
            31.2371,30.0459,25.0
            31.2397,30.0476,26.6
            31.2429,30.0491,28.2
            31.2464,30.0502,29.9
            31.2504,30.0511,31.5
            31.2547,30.0516,32.8
            31.2591,30.0519,34.0
          </coordinates>
        </LineString>
      </Placemark>
    </Folder>
    <Folder>
      <name>Survey_Stations</name>
      <Placemark>
        <name>STA 0+000</name>
        <description>Start of Project - Elevation 24.50m</description>
        <Point>
          <coordinates>31.2350,30.0440,24.5</coordinates>
        </Point>
      </Placemark>
      <Placemark>
        <name>STA 0+200</name>
        <description>Station 0+200 - Elevation 25.20m</description>
        <Point>
          <coordinates>31.2372,30.0458,25.2</coordinates>
        </Point>
      </Placemark>
      <Placemark>
        <name>STA 0+500</name>
        <description>Station 0+500 Point of Curvature (PC)</description>
        <Point>
          <coordinates>31.2398,30.0475,26.8</coordinates>
        </Point>
      </Placemark>
      <Placemark>
        <name>STA 0+800</name>
        <description>Station 0+800 Point of Tangency (PT)</description>
        <Point>
          <coordinates>31.2430,30.0490,28.4</coordinates>
        </Point>
      </Placemark>
      <Placemark>
        <name>STA 1+200</name>
        <description>Station 1+200 Culvert Location</description>
        <Point>
          <coordinates>31.2505,30.0510,31.7</coordinates>
        </Point>
      </Placemark>
      <Placemark>
        <name>STA 1+600</name>
        <description>End of Project Section - Elevation 34.20m</description>
        <Point>
          <coordinates>31.2592,30.0518,34.2</coordinates>
        </Point>
      </Placemark>
    </Folder>
  </Document>
</kml>
""".trimIndent()
        ),
        SampleProject(
            id = "cadastral_parcels",
            titleEn = "Cadastral Land Subdivision",
            titleAr = "مخطط مساحي وتقسيم أراضي (Cadastral)",
            descriptionEn = "Land parcel boundaries (Lots 101-104), road reservation, and boundary corner control monuments",
            descriptionAr = "حدود قطع أراضي سكنية مع علامات أركان القطع وشوارع التخديم وأرقام القطع",
            kmlContent = """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Al-Nakheel Residential Subdivision</name>
    <description>Cadastral Survey Plan - Block 14</description>
    <Folder>
      <name>Parcels</name>
      <Placemark>
        <name>LOT_101</name>
        <description>Area: 650.0 sq.m - Residential Zone A</description>
        <Polygon>
          <outerBoundaryIs>
            <LinearRing>
              <coordinates>
                31.3000,30.0800,18.0
                31.3005,30.0800,18.1
                31.3005,30.0805,18.3
                31.3000,30.0805,18.2
                31.3000,30.0800,18.0
              </coordinates>
            </LinearRing>
          </outerBoundaryIs>
        </Polygon>
      </Placemark>
      <Placemark>
        <name>LOT_102</name>
        <description>Area: 650.0 sq.m - Residential Zone A</description>
        <Polygon>
          <outerBoundaryIs>
            <LinearRing>
              <coordinates>
                31.3005,30.0800,18.1
                31.3010,30.0800,18.2
                31.3010,30.0805,18.4
                31.3005,30.0805,18.3
                31.3005,30.0800,18.1
              </coordinates>
            </LinearRing>
          </outerBoundaryIs>
        </Polygon>
      </Placemark>
      <Placemark>
        <name>LOT_103</name>
        <description>Area: 720.0 sq.m - Corner Plot</description>
        <Polygon>
          <outerBoundaryIs>
            <LinearRing>
              <coordinates>
                31.3010,30.0800,18.2
                31.3016,30.0800,18.3
                31.3016,30.0805,18.5
                31.3010,30.0805,18.4
                31.3010,30.0800,18.2
              </coordinates>
            </LinearRing>
          </outerBoundaryIs>
        </Polygon>
      </Placemark>
      <Placemark>
        <name>PUBLIC_PARK</name>
        <description>Green Area and Playground</description>
        <Polygon>
          <outerBoundaryIs>
            <LinearRing>
              <coordinates>
                31.3000,30.0807,18.3
                31.3016,30.0807,18.6
                31.3016,30.0812,18.7
                31.3000,30.0812,18.4
                31.3000,30.0807,18.3
              </coordinates>
            </LinearRing>
          </outerBoundaryIs>
        </Polygon>
      </Placemark>
    </Folder>
    <Folder>
      <name>Boundary_Monuments</name>
      <Placemark>
        <name>CP-101A</name>
        <description>Concrete Boundary Pillar #1</description>
        <Point><coordinates>31.3000,30.0800,18.0</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>CP-101B</name>
        <description>Concrete Boundary Pillar #2</description>
        <Point><coordinates>31.3005,30.0800,18.1</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>CP-102A</name>
        <description>Concrete Boundary Pillar #3</description>
        <Point><coordinates>31.3010,30.0800,18.2</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>CP-103_CORNER</name>
        <description>Corner Survey Monument #4</description>
        <Point><coordinates>31.3016,30.0800,18.3</coordinates></Point>
      </Placemark>
    </Folder>
  </Document>
</kml>
""".trimIndent()
        ),
        SampleProject(
            id = "water_pipeline",
            titleEn = "Water Pipeline & Valve Network",
            titleAr = "شبكة مياه ومحابس تحكم (Utilities)",
            descriptionEn = "Ductile iron main pipeline DN400, gate valves, pressure reducing station, and fire hydrants",
            descriptionAr = "خط أنابيب رئيسي قطر 400مم ومحابس قفل وتخفيض ضغط وغرف تفتيش وحنفيات حريق",
            kmlContent = """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Main Water Feeder Network DN400</name>
    <Folder>
      <name>Pipelines</name>
      <Placemark>
        <name>MAIN_PIPE_DN400</name>
        <description>Ductile Iron Pipe Class K9 - Depth 1.5m</description>
        <LineString>
          <coordinates>
            31.2100,29.9800,15.2
            31.2120,29.9815,15.8
            31.2145,29.9830,16.4
            31.2170,29.9838,17.1
            31.2200,29.9842,17.8
            31.2235,29.9845,18.5
          </coordinates>
        </LineString>
      </Placemark>
      <Placemark>
        <name>BRANCH_PIPE_DN200</name>
        <description>Secondary Feeder DN200 to Sector B</description>
        <LineString>
          <coordinates>
            31.2145,29.9830,16.4
            31.2148,29.9855,16.9
            31.2152,29.9880,17.4
          </coordinates>
        </LineString>
      </Placemark>
    </Folder>
    <Folder>
      <name>Valves_and_Fittings</name>
      <Placemark>
        <name>GV-01</name>
        <description>Gate Valve Chamber DN400 - Invert: 13.70m</description>
        <Point><coordinates>31.2100,29.9800,15.2</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>PRV-01</name>
        <description>Pressure Reducing Valve Chamber</description>
        <Point><coordinates>31.2145,29.9830,16.4</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>AV-02</name>
        <description>Air Release Valve High Point</description>
        <Point><coordinates>31.2200,29.9842,17.8</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>FH-104</name>
        <description>Fire Hydrant Double Outlet</description>
        <Point><coordinates>31.2148,29.9855,16.9</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>END_CAP</name>
        <description>Thrust Block and Blind Flange</description>
        <Point><coordinates>31.2235,29.9845,18.5</coordinates></Point>
      </Placemark>
    </Folder>
  </Document>
</kml>
""".trimIndent()
        ),
        SampleProject(
            id = "building_grid",
            titleEn = "Construction Site & Building Grid",
            titleAr = "موقع مشروع ومحاور الأعمدة (Structural Grid)",
            descriptionEn = "Building outline, primary column grid axes (A-D, 1-4), and site temporary benchmark (TBM)",
            descriptionAr = "حدود المبنى الخارجي مع شبكة المحاور الإنشائية ونقطة الثابت المساحي المرجعي (TBM)",
            kmlContent = """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Administrative Tower - Site Survey</name>
    <Folder>
      <name>Building_Footprint</name>
      <Placemark>
        <name>TOWER_FOOTPRINT</name>
        <description>Cast-in-place Concrete Raft Boundary</description>
        <Polygon>
          <outerBoundaryIs>
            <LinearRing>
              <coordinates>
                31.25000,30.06000,45.0
                31.25040,30.06000,45.0
                31.25040,30.06035,45.0
                31.25000,30.06035,45.0
                31.25000,30.06000,45.0
              </coordinates>
            </LinearRing>
          </outerBoundaryIs>
        </Polygon>
      </Placemark>
    </Folder>
    <Folder>
      <name>Grid_Lines</name>
      <Placemark>
        <name>GRID_AXIS_A</name>
        <description>Column Grid Axis A</description>
        <LineString>
          <coordinates>
            31.24995,30.06000,45.0
            31.25045,30.06000,45.0
          </coordinates>
        </LineString>
      </Placemark>
      <Placemark>
        <name>GRID_AXIS_B</name>
        <description>Column Grid Axis B</description>
        <LineString>
          <coordinates>
            31.24995,30.06015,45.0
            31.25045,30.06015,45.0
          </coordinates>
        </LineString>
      </Placemark>
      <Placemark>
        <name>GRID_AXIS_C</name>
        <description>Column Grid Axis C</description>
        <LineString>
          <coordinates>
            31.24995,30.06030,45.0
            31.25045,30.06030,45.0
          </coordinates>
        </LineString>
      </Placemark>
    </Folder>
    <Folder>
      <name>Control_Points</name>
      <Placemark>
        <name>TBM-01</name>
        <description>Temporary Benchmark - Elevation 45.000m</description>
        <Point><coordinates>31.24980,30.05980,45.000</coordinates></Point>
      </Placemark>
      <Placemark>
        <name>TBM-02</name>
        <description>Secondary Benchmark - Elevation 45.120m</description>
        <Point><coordinates>31.25060,30.06050,45.120</coordinates></Point>
      </Placemark>
    </Folder>
  </Document>
</kml>
""".trimIndent()
        )
    )
}
