package com.bluetutor.android.feature.preview

data class PreviewUiState(
    val userName: String,
    val streakDays: Int,
    val weeklyPreviewedLessons: Int,
    val weeklyGoalCurrent: Int,
    val weeklyGoalTarget: Int,
    val quickTopics: List<PreviewQuickTopicUiModel>,
    val quickEntries: List<PreviewQuickEntryUiModel>,
    val recommendedLessons: List<PreviewRecommendedLessonUiModel>,
    val uploadCard: PreviewUploadCardUiModel,
)

data class PreviewQuickTopicUiModel(
    val id: Int,
    val emoji: String,
    val label: String,
    val grade: String,
    val intro: String,
    val seedContent: String,
    val handout: PreviewHandoutUiModel,
)

data class PreviewHandoutUiModel(
    val articleTitle: String,
    val articleSubtitle: String,
    val introduction: String,
    val blocks: List<PreviewHandoutBlockUiModel>,
    val footerPrompt: String,
)

data class PreviewHandoutBlockUiModel(
    val id: String,
    val type: PreviewHandoutBlockType,
    val title: String = "",
    val text: String,
    val supportingText: String = "",
    val sectionTitle: String = "",
)

enum class PreviewHandoutBlockType {
    SectionHeading,
    Paragraph,
    Formula,
    ThinkingPrompt,
    Note,
}

enum class PreviewQuickEntryTone {
    Sky,
    Warm,
}

data class PreviewQuickEntryUiModel(
    val id: Int,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val tone: PreviewQuickEntryTone,
)

data class PreviewRecommendedLessonUiModel(
    val id: Int,
    val topicId: Int,
    val tag: String,
    val grade: String,
    val title: String,
    val description: String,
    val masteryPercent: Int,
)

enum class PreviewUploadStage {
    Idle,
    Processing,
    Success,
}

data class PreviewUploadCardUiModel(
    val stage: PreviewUploadStage,
    val title: String,
    val description: String,
    val fileName: String?,
    val helperText: String?,
)

fun previewMockUiState(
    uploadStage: PreviewUploadStage,
    uploadedFileName: String?,
): PreviewUiState {
    val uploadCard = when (uploadStage) {
        PreviewUploadStage.Idle -> PreviewUploadCardUiModel(
            stage = uploadStage,
            title = "上传讲义 / 教材",
            description = "AI 自动提取知识点，生成专属预习计划，智能追问引导。",
            fileName = null,
            helperText = null,
        )

        PreviewUploadStage.Processing -> PreviewUploadCardUiModel(
            stage = uploadStage,
            title = "AI 正在分析讲义",
            description = "文档已接收，正在提取知识点和重点公式。",
            fileName = uploadedFileName,
            helperText = null,
        )

        PreviewUploadStage.Success -> PreviewUploadCardUiModel(
            stage = uploadStage,
            title = "上传成功！🎉",
            description = "讲义已经完成初步整理，可以开始今日预习。",
            fileName = uploadedFileName,
            helperText = null,
        )
    }

    val quickTopics = listOf(
            PreviewQuickTopicUiModel(
                id = 1,
                emoji = "📐",
                label = "图形面积",
                grade = "五年级到六年级",
                intro = "先理解常见平面图形的面积公式，再学会根据条件灵活选公式。",
                seedContent = "图形面积专题主要包括长方形、正方形、平行四边形、三角形和梯形的面积计算。学习时要先理解底、高与面积之间的关系，再记住不同图形的面积公式，并注意单位换算和高的正确寻找。",
                handout = buildAreaHandout(),
            ),
            PreviewQuickTopicUiModel(
                id = 2,
                emoji = "🔢",
                label = "分数运算",
                grade = "五年级到六年级",
                intro = "理解分数的意义、约分通分规则，以及分数四则运算的核心方法。",
                seedContent = "分数运算专题主要包括分数的意义、约分、通分、分数加减法、分数乘法和分数除法。预习时要先理解分母相同和分母不同的处理方式，再掌握分数乘除法和整数、小数之间的联系，尤其注意先约分再计算可以让过程更简洁。",
                handout = buildFractionHandout(),
            ),
            PreviewQuickTopicUiModel(
                id = 3,
                emoji = "🚂",
                label = "行程问题",
                grade = "六年级",
                intro = "抓住速度、时间、路程三者关系，并学会借助线段图理解相遇和追及。",
                seedContent = "行程问题专题主要研究速度、时间和路程三者之间的关系。学习时要先掌握路程等于速度乘时间、速度等于路程除以时间、时间等于路程除以速度，再理解相遇问题、追及问题和往返问题中总路程与相对速度的变化。画线段图有助于理解题意。",
                handout = buildTravelHandout(),
            ),
            PreviewQuickTopicUiModel(
                id = 4,
                emoji = "📊",
                label = "数据统计",
                grade = "四年级到六年级",
                intro = "先认识平均数、统计图和数据整理方法，再理解数据背后的变化趋势。",
                seedContent = "数据统计专题主要包括数据收集、整理、统计表、条形统计图、折线统计图和平均数。预习时要先理解不同统计图适合表示什么信息，再学会从图表中读取数据、比较大小规律，并用平均数描述整体水平。",
                handout = buildStatisticsHandout(),
            ),
            PreviewQuickTopicUiModel(
                id = 5,
                emoji = "🧮",
                label = "方程",
                grade = "五年级到初中",
                intro = "先理解等式与未知数，再学习用方程表达数量关系。",
                seedContent = "方程专题主要包括等式、未知数、一元一次方程以及利用方程解决简单实际问题。预习时要先理解方程表示的是相等关系，再掌握移项、合并同类项和检验结果的方法，注意把文字关系正确翻译成含未知数的式子。",
                handout = buildEquationHandout(),
            ),
        )

    val recommendedLessons = quickTopics
        .shuffled()
        .take(2)
        .mapIndexed { index, topic ->
            PreviewRecommendedLessonUiModel(
                id = index + 1,
                topicId = topic.id,
                tag = "快捷预习",
                grade = topic.grade,
                title = topic.label,
                description = topic.intro,
                masteryPercent = listOf(42, 58, 67, 73, 81)[index % 5],
            )
        }

    return PreviewUiState(
        userName = "小明同学",
        streakDays = 7,
        weeklyPreviewedLessons = 3,
        weeklyGoalCurrent = 3,
        weeklyGoalTarget = 5,
        quickTopics = quickTopics,
        quickEntries = listOf(
            PreviewQuickEntryUiModel(1, "📷", "拍照提问", "一步步引导你想", PreviewQuickEntryTone.Sky),
            PreviewQuickEntryUiModel(2, "📚", "选知识点", "系统性预习", PreviewQuickEntryTone.Warm),
        ),
        recommendedLessons = recommendedLessons,
        uploadCard = uploadCard,
    )
}

private fun buildAreaHandout(): PreviewHandoutUiModel {
    return PreviewHandoutUiModel(
        articleTitle = "图形面积讲义",
        articleSubtitle = "先认识图形，再理解底、高和面积之间的关系",
        introduction = "图形面积问题是小学数学里非常常见的一类题目。它表面上是在套公式，实际上更重要的是学会观察图形结构，分清底和高，并知道什么时候需要拆分图形、什么时候要先统一单位。",
        blocks = listOf(
            PreviewHandoutBlockUiModel(
                id = "area_section_1",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "一、什么是图形面积问题？",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_p_1",
                type = PreviewHandoutBlockType.Paragraph,
                text = "图形面积问题主要研究一个图形占了多大平面区域。常见图形有长方形、正方形、平行四边形、三角形和梯形。真正做题时，第一步往往不是马上代公式，而是先判断图形属于哪一类，再去找和它对应的面积关系。",
                sectionTitle = "一、什么是图形面积问题？",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_section_2",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "二、核心公式（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_formula_1",
                type = PreviewHandoutBlockType.Formula,
                title = "常见面积公式",
                text = "长方形面积 = 长 × 宽\n正方形面积 = 边长 × 边长\n平行四边形面积 = 底 × 高\n三角形面积 = 底 × 高 ÷ 2\n梯形面积 = （上底 + 下底）× 高 ÷ 2",
                supportingText = "提示：公式虽然不同，但它们都和“底、高、图形拆拼”有关。很多面积公式其实都能通过拼图或剪图推导出来。",
                sectionTitle = "二、核心公式（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_section_3",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "三、常见情况及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_p_2",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第一种常见情况，是直接求规则图形的面积。遇到这类题，先判断图形，再找和底对应的高。尤其是平行四边形、三角形和梯形，高必须和所选的底互相垂直，不能把斜边误当成高。",
                sectionTitle = "三、常见情况及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_note_1",
                type = PreviewHandoutBlockType.Note,
                title = "规则图形的易错点",
                text = "梯形的两条腰通常不是高；平行四边形如果换了一条底，和它配对的高也会跟着变化。",
                sectionTitle = "三、常见情况及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_p_3",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第二种常见情况，是求组合图形的面积。碰到 L 形、凹形或带缺口的图形时，不需要硬背新公式，更稳妥的办法是先拆成熟悉的图形，再分别求面积后相加，或者先算大图形再减去缺掉的部分。",
                sectionTitle = "三、常见情况及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_note_2",
                type = PreviewHandoutBlockType.Note,
                title = "组合图形提示",
                text = "如果一眼看不出怎么拆，可以先补辅助线，把它变成熟悉的长方形、三角形或梯形。",
                sectionTitle = "三、常见情况及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_section_4",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_p_4",
                type = PreviewHandoutBlockType.Paragraph,
                text = "1. 先认图形，判断属于哪一类。\n2. 圈出底和高，确认是否互相垂直。\n3. 看是否需要统一单位。\n4. 如果是组合图形，先拆或先补辅助线。\n5. 代入公式后再检查结果是否合理。",
                sectionTitle = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_section_5",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_prompt_1",
                type = PreviewHandoutBlockType.ThinkingPrompt,
                title = "先自己试一试",
                text = "1. 一个长方形长 8 厘米，宽 5 厘米，面积是多少？\n2. 一个三角形底是 10 厘米，高是 6 厘米，面积是多少？\n3. 一个 L 形图形可以拆成两个长方形，面积分别是 24 平方厘米和 16 平方厘米，总面积是多少？",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_note_3",
                type = PreviewHandoutBlockType.Note,
                title = "答案",
                text = "1. 40 平方厘米\n2. 30 平方厘米\n3. 40 平方厘米",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_section_6",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "六、学习小贴士",
            ),
            PreviewHandoutBlockUiModel(
                id = "area_note_4",
                type = PreviewHandoutBlockType.Note,
                title = "记住这三点",
                text = "面积题最怕认错图形和找错高。\n多画辅助线，往往比多背一个公式更有用。\n看到单位不一样时，先统一单位再计算。",
                sectionTitle = "六、学习小贴士",
            ),
        ),
        footerPrompt = "面积题的关键不是背更多公式，而是认清图形和底高关系。哪一步你最容易看错，就把那一段带去问 AI。",
    )
}

private fun buildFractionHandout(): PreviewHandoutUiModel {
    return PreviewHandoutUiModel(
        articleTitle = "分数运算讲义",
        articleSubtitle = "先理解分数的意义，再掌握通分、约分和乘除法",
        introduction = "分数运算是小学高年级的重要内容。很多同学觉得它难，往往不是因为不会算，而是没有先想明白分数表示什么、为什么要通分、什么时候应该先约分。只要把这些关系理顺，计算会顺很多。",
        blocks = listOf(
            PreviewHandoutBlockUiModel(
                id = "fraction_section_1",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "一、什么是分数运算？",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_p_1",
                type = PreviewHandoutBlockType.Paragraph,
                text = "分数运算研究的是“同一个整体被分成若干份之后，份数之间怎样合并、比较和变化”。分母表示把整体平均分成几份，分子表示取了其中几份。理解了这一点，后面的通分、约分和乘倒数才不会只剩下死记规则。",
                sectionTitle = "一、什么是分数运算？",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_section_2",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "二、核心规则（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_formula_1",
                type = PreviewHandoutBlockType.Formula,
                title = "分数运算最常见的规则",
                text = "分母相同，分数加减时可以直接加减分子。\n分母不同，先通分，再做加减。\n分数乘法可以先约分后再乘。\n分数除法要先改成乘对方的倒数。",
                supportingText = "提示：通分是把不同大小的“份”变成同一种单位；约分是把结果化得更简洁。",
                sectionTitle = "二、核心规则（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_section_3",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_p_2",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第一类常见题，是分数加减法。遇到这类题，先看分母是否相同；如果不同，就先通分。比如 1/2 和 1/3 看起来都只有一份，但这两份的大小根本不同，所以不能直接相加。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_formula_2",
                type = PreviewHandoutBlockType.Formula,
                title = "乘除法的常见处理",
                text = "分数乘法先看能不能约分。\n分数除法先改成乘法，再把除数翻成倒数。",
                supportingText = "例如：2/3 ÷ 4/5 = 2/3 × 5/4。这样处理以后，再约分会更顺手。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_note_1",
                type = PreviewHandoutBlockType.Note,
                title = "易错提醒",
                text = "分母不同却直接相加减、结果没有继续化简、分数除法忘了乘倒数，都是最常见的错误。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_section_4",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_p_3",
                type = PreviewHandoutBlockType.Paragraph,
                text = "1. 先判断是加减法还是乘除法。\n2. 加减法先看要不要通分，乘除法先看能不能约分。\n3. 写完结果后检查能不能继续化简。\n4. 如果题目和整数、小数混合，先统一表示方式再算。",
                sectionTitle = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_section_5",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_prompt_1",
                type = PreviewHandoutBlockType.ThinkingPrompt,
                title = "先自己试一试",
                text = "1. 1/4 + 3/4 = ?\n2. 2/3 + 1/6 = ?\n3. 5/8 × 4/5 = ?\n4. 3/4 ÷ 1/2 = ?",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_note_2",
                type = PreviewHandoutBlockType.Note,
                title = "答案",
                text = "1. 1\n2. 5/6\n3. 1/2\n4. 3/2",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_section_6",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "六、学习小贴士",
            ),
            PreviewHandoutBlockUiModel(
                id = "fraction_note_3",
                type = PreviewHandoutBlockType.Note,
                title = "记住这三点",
                text = "分母决定每份有多大，所以分母不同不能直接加减。\n能先约分就先约分，计算会短很多。\n分数除法别硬背，先想成“乘对方的倒数”。",
                sectionTitle = "六、学习小贴士",
            ),
        ),
        footerPrompt = "分数运算最怕一上来就机械套规则。哪一步你最不理解，是通分、约分还是乘倒数，就直接带着那一段去问 AI。",
    )
}

private fun buildTravelHandout(): PreviewHandoutUiModel {
    return PreviewHandoutUiModel(
        articleTitle = "行程问题讲义",
        articleSubtitle = "理解路程、速度、时间的关系，再分清不同题型的解法",
        introduction = "行程问题是小学数学里一类很典型的应用题。它研究的是物体在匀速运动过程中，路程、速度与时间三者之间的数量关系。因为情景接近日常生活，比如走路、骑车、坐车和跑步，所以它不仅常考，也特别适合训练分析过程和数量关系。",
        blocks = listOf(
            PreviewHandoutBlockUiModel(
                id = "travel_section_1",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "一、什么是行程问题？",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_p_1",
                type = PreviewHandoutBlockType.Paragraph,
                text = "行程问题的核心不在于题目写得多复杂，而在于你能不能看清楚：谁在动、动了多久、速度是多少、距离怎样变化。只要把这几个问题理顺，大多数题目都能回到一个简单框架里处理。",
                sectionTitle = "一、什么是行程问题？",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_2",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "二、核心公式（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_formula_1",
                type = PreviewHandoutBlockType.Formula,
                title = "行程问题的基本公式",
                text = "路程 = 速度 × 时间\n速度 = 路程 ÷ 时间\n时间 = 路程 ÷ 速度",
                supportingText = "提示：做题前务必统一单位。例如速度如果用“米/分”，时间就要用“分”；如果速度是“千米/小时”，时间就要用“小时”。",
                sectionTitle = "二、核心公式（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_3",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_3_1",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "相遇问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_p_2",
                type = PreviewHandoutBlockType.Paragraph,
                text = "情景：两人或两车从两地同时出发，相向而行，最后相遇。\n特点：两个人共同走完整个路程。\n做题时通常先找速度和，再把总路程和相遇时间联系起来。",
                sectionTitle = "相遇问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_formula_2",
                type = PreviewHandoutBlockType.Formula,
                title = "核心公式",
                text = "总路程 = （甲速 + 乙速）× 相遇时间\n相遇时间 = 总路程 ÷ （甲速 + 乙速）",
                supportingText = "例题：小明和小红分别从 A、B 两地同时出发，相向而行。小明每分钟走 60 米，小红每分钟走 40 米，两地相距 1000 米。\n解：速度和 = 60 + 40 = 100（米/分），相遇时间 = 1000 ÷ 100 = 10（分钟）。",
                sectionTitle = "相遇问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_3_2",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "追及问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_p_3",
                type = PreviewHandoutBlockType.Paragraph,
                text = "情景：两人同向而行，快者追赶慢者。\n特点：一开始就存在路程差，后面快者逐渐缩小差距，直到追上。\n这种题最关键的是先看清谁先出发、谁后出发，以及起始差距有多大。",
                sectionTitle = "追及问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_formula_3",
                type = PreviewHandoutBlockType.Formula,
                title = "核心公式",
                text = "追及时间 = 路程差 ÷ 速度差\n路程差 = 速度差 × 追及时间",
                supportingText = "例题：小刚先出发 5 分钟，每分钟走 50 米；小强随后出发，每分钟走 70 米。\n解：小刚先走的路程 = 50 × 5 = 250（米），速度差 = 70 - 50 = 20（米/分），追及时间 = 250 ÷ 20 = 12.5（分钟）。",
                sectionTitle = "追及问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_3_3",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "相背问题（背向而行）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_p_4",
                type = PreviewHandoutBlockType.Paragraph,
                text = "情景：两人从同一地点或两地背向出发，距离越来越远。\n判断这类题时，可以把它理解成“每经过 1 个时间单位，两人之间会一起拉开多少距离”。",
                sectionTitle = "相背问题（背向而行）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_formula_4",
                type = PreviewHandoutBlockType.Formula,
                title = "核心公式",
                text = "两人距离 = （甲速 + 乙速）× 时间",
                supportingText = "例题：甲、乙两人从同一地点背向而行，甲每小时走 5 千米，乙每小时走 4 千米。3 小时后两人相距多少千米？\n解：距离 = （5 + 4）× 3 = 27（千米）。",
                sectionTitle = "相背问题（背向而行）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_3_4",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "环形跑道问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_note_1",
                type = PreviewHandoutBlockType.Note,
                title = "思路提醒",
                text = "背向相遇时，可以理解为两人一共跑完一圈：时间 = 圈长 ÷ 速度和。\n同向追及时，可以理解为快者比慢者多跑一圈：时间 = 圈长 ÷ 速度差。",
                sectionTitle = "环形跑道问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_3_5",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "火车过桥问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_note_2",
                type = PreviewHandoutBlockType.Note,
                title = "关键点",
                text = "火车完全通过桥时，车头不只是走了桥长，还要把整列火车带过去，所以总路程 = 桥长 + 车长 = 速度 × 时间。",
                sectionTitle = "火车过桥问题",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_4",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_p_5",
                type = PreviewHandoutBlockType.Paragraph,
                text = "1. 读题：先看谁在动，方向如何，是否同时出发。\n2. 画图：用线段图标出位置、方向和已知距离。\n3. 找关系：判断是相遇、追及、相背，还是其他特殊类型。\n4. 列式计算：套用对应公式，注意单位统一。\n5. 检验答案：回到原题，看结果是否合理。",
                sectionTitle = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_5",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_prompt_1",
                type = PreviewHandoutBlockType.ThinkingPrompt,
                title = "先自己试一试",
                text = "1. 甲、乙两地相距 240 千米，一辆汽车从甲地开往乙地，每小时行 60 千米，几小时到达？\n2. 小华和小丽从相距 800 米的两地同时出发，相向而行。小华每分钟走 70 米，小丽每分钟走 90 米。几分钟后相遇？\n3. 弟弟以每分钟 60 米的速度步行上学，5 分钟后哥哥骑车以每分钟 210 米的速度追赶。哥哥几分钟能追上弟弟？",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_note_3",
                type = PreviewHandoutBlockType.Note,
                title = "答案",
                text = "1. 4 小时\n2. 5 分钟\n3. 2 分钟",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_section_6",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "六、学习小贴士",
            ),
            PreviewHandoutBlockUiModel(
                id = "travel_note_4",
                type = PreviewHandoutBlockType.Note,
                title = "记住这三点",
                text = "行程问题本质上研究的是“运动中的数量关系”，理解情景比死记公式更重要。\n多画图、多模拟实际场景，可以更快建立距离变化的直觉。\n遇到复杂问题，比如多次相遇或环形跑道，别急着一次算完，分阶段分析通常更稳。",
                sectionTitle = "六、学习小贴士",
            ),
        ),
        footerPrompt = "做行程题时，先判断距离是在变小还是变大，再决定该用速度和还是速度差。哪一类题你最容易混，就把那一段带去问 AI。",
    )
}

private fun buildStatisticsHandout(): PreviewHandoutUiModel {
    return PreviewHandoutUiModel(
        articleTitle = "数据统计讲义",
        articleSubtitle = "先看图表表达什么，再决定如何读取和计算",
        introduction = "数据统计问题看起来像在读表和算平均数，但真正重要的是先看懂图表在表达什么。只要先弄清楚是比较大小、观察变化，还是描述整体水平，后面的读图和计算就会清楚很多。",
        blocks = listOf(
            PreviewHandoutBlockUiModel(
                id = "stats_section_1",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "一、什么是数据统计？",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_p_1",
                type = PreviewHandoutBlockType.Paragraph,
                text = "数据统计主要研究怎样整理数据、展示数据，并从数据中读出有用的信息。小学阶段最常见的内容有统计表、条形统计图、折线统计图和平均数。做题时不要急着算，先想清楚这张图表到底想告诉你什么。",
                sectionTitle = "一、什么是数据统计？",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_section_2",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "二、核心概念（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_formula_1",
                type = PreviewHandoutBlockType.Formula,
                title = "平均数的基础关系",
                text = "平均数 = 总量 ÷ 总份数",
                supportingText = "提示：平均数表示整体水平，不代表每个数据都一样。看到平均数时，要先想到“总量被平均分配以后，每一份是多少”。",
                sectionTitle = "二、核心概念（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_p_2",
                type = PreviewHandoutBlockType.Paragraph,
                text = "统计表适合完整记录数据；条形图更适合比较大小；折线图更适合观察变化趋势。分清图表类型，是读统计题的第一步。",
                sectionTitle = "二、核心概念（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_section_3",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_p_3",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第一类常见题，是读统计表和条形图。做这类题时，先看横轴、纵轴分别表示什么，再注意单位和每一格代表多少。很多错误不是算错，而是读错了图。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_note_1",
                type = PreviewHandoutBlockType.Note,
                title = "读图提醒",
                text = "条形图和折线图最容易出错的地方，是没有先看清横轴、纵轴和单位。有时候一格代表 1，有时候一格代表 5。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_p_4",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第二类常见题，是观察变化趋势。遇到折线图时，可以先找最高点、最低点、上升最快和下降最快的部分，再去回答问题。这样能更快抓住整张图的主线。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_p_5",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第三类常见题，是求平均数。做这类题时，不要把平均数当成“每个数据都一样”，而是要先求出总量，再除以份数。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_section_4",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_p_6",
                type = PreviewHandoutBlockType.Paragraph,
                text = "1. 先看图表类型。\n2. 再看横轴、纵轴和单位。\n3. 读出关键数据，比较大小规律或变化方向。\n4. 如果要求平均数，先求总量再平均分。\n5. 最后回头检查有没有读错单位。",
                sectionTitle = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_section_5",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_prompt_1",
                type = PreviewHandoutBlockType.ThinkingPrompt,
                title = "先自己试一试",
                text = "1. 三位同学的跳绳成绩分别是 120、135、105，下平均每人多少个？\n2. 如果一周气温数据想看变化趋势，更适合用条形图还是折线图？\n3. 读统计图时，为什么要先看单位和每一格代表什么？",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_note_2",
                type = PreviewHandoutBlockType.Note,
                title = "答案",
                text = "1. 120 个\n2. 折线图\n3. 因为单位没看清，读出来的数据大小就可能全错。",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_section_6",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "六、学习小贴士",
            ),
            PreviewHandoutBlockUiModel(
                id = "stats_note_3",
                type = PreviewHandoutBlockType.Note,
                title = "记住这三点",
                text = "统计题不要急着算，先说清楚图表在表达什么。\n折线图看趋势，条形图看大小，统计表看完整数据。\n平均数表示整体水平，不代表每个数据都一样。",
                sectionTitle = "六、学习小贴士",
            ),
        ),
        footerPrompt = "统计题最怕一上来就埋头算。你如果不确定某张图到底在表达什么，就直接把对应那一段带去问 AI。",
    )
}

private fun buildEquationHandout(): PreviewHandoutUiModel {
    return PreviewHandoutUiModel(
        articleTitle = "方程讲义",
        articleSubtitle = "先找到相等关系，再用未知数把题意写清楚",
        introduction = "方程是小学高年级到初中都会反复遇到的重要内容。很多同学一想到方程就只想到移项和求解，但真正关键的第一步，是先找到题目里的相等关系，再用未知数把这个关系表示出来。",
        blocks = listOf(
            PreviewHandoutBlockUiModel(
                id = "equation_section_1",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "一、什么是方程？",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_p_1",
                type = PreviewHandoutBlockType.Paragraph,
                text = "方程是一种表示“左右两边相等”的数学式子，其中通常会有未知数。它的作用不是单纯让计算更快，而是帮助我们把题目里的数量关系准确地写出来。",
                sectionTitle = "一、什么是方程？",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_section_2",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "二、核心概念（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_formula_1",
                type = PreviewHandoutBlockType.Formula,
                title = "最基础的等式感",
                text = "x + 5 = 12 表示：某个数加 5 以后，结果和 12 相等。",
                supportingText = "提示：方程左右两边表示同一件事，所以对一边做什么，另一边也要做对应变化。",
                sectionTitle = "二、核心概念（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_p_2",
                type = PreviewHandoutBlockType.Paragraph,
                text = "列方程前，最好先用一句自然语言说清楚：题目里究竟是哪两个量相等。很多应用题不是不会算，而是“比……多”“比……少”“一共”这些关系没有翻译清楚。",
                sectionTitle = "二、核心概念（基础）",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_section_3",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_p_3",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第一类常见题，是直接解简单方程。遇到这类题，要先看未知数在等式哪一边，再用“保持平衡”的想法一步步化简。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_p_4",
                type = PreviewHandoutBlockType.Paragraph,
                text = "第二类常见题，是应用题列方程。做这类题时别急着写符号，先把“谁和谁相等”说出来，再决定未知数设给谁。比如“一个数的 3 倍比 18 少 6”，可以先翻译成“某个数乘 3 以后，结果和 18 - 6 相等”。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_note_1",
                type = PreviewHandoutBlockType.Note,
                title = "易错提醒",
                text = "设未知数时别只写 x，要想清楚它代表什么量；解完之后别忘了代回原式检查。",
                sectionTitle = "三、常见类型及解题方法",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_section_4",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_p_5",
                type = PreviewHandoutBlockType.Paragraph,
                text = "1. 先设未知数，并说清它表示什么。\n2. 找出题目里的相等关系。\n3. 根据关系列出方程。\n4. 一步步求解。\n5. 把结果代回原题检验。",
                sectionTitle = "四、解题步骤建议",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_section_5",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_prompt_1",
                type = PreviewHandoutBlockType.ThinkingPrompt,
                title = "先自己试一试",
                text = "1. 解方程：x + 7 = 19\n2. 解方程：3x = 27\n3. 一个数的 2 倍加上 5 等于 21，这个数是多少？",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_note_2",
                type = PreviewHandoutBlockType.Note,
                title = "答案",
                text = "1. x = 12\n2. x = 9\n3. x = 8",
                sectionTitle = "五、练习题（自测）",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_section_6",
                type = PreviewHandoutBlockType.SectionHeading,
                text = "六、学习小贴士",
            ),
            PreviewHandoutBlockUiModel(
                id = "equation_note_3",
                type = PreviewHandoutBlockType.Note,
                title = "记住这三点",
                text = "列方程前先找“谁和谁相等”。\n应用题里设未知数时要写清它表示什么。\n解完后代回去检验，能帮你发现很多隐藏错误。",
                sectionTitle = "六、学习小贴士",
            ),
        ),
        footerPrompt = "方程最难的通常不是解，而是把题意翻译成相等关系。你如果卡在列式那一步，就直接带着那一句去问 AI。",
    )
}