# CET-4 构词组合图谱(前缀 × 词根 × 后缀)

> 引擎:P{0-2} 前缀 + R{1-2} 词根 + S{0-2} 后缀,对词表 6190 词全扫描;最简拆解优先;错误词源已人工剔除。
> 共 **2432** 个表内单词可由构词成分组合而成。图例:**P**=前缀 **R**=词根 **S**=后缀 **(e)**=动词尾 e

## 一、构词模式统计

| 模式 | 词数 | 例 |
|---|---|---|
| P+R+S | 745 | education, prediction |
| R+S | 540 | action, dictation, actor |
| P+R | 443 | predict, adopt, absorb |
| R+S+S | 145 | national, ability |
| R+R+S | 141 | agriculture, manufacture |
| R+R | 74 | hydrogen, democracy |
| P+R+S+S | 63 | national |
| P+P+R+S | 42 | recommendation, indispensable |
| P+P+R | 33 | uniform |
| P+R+R+S | 26 | biotechnology 类 |
| P+R+R | 20 | divide? abstract |
| R+R+S+S | 12 | satisfaction 类 |
| R+R(e) | 9 | creature, feature |
| P+P+R+R | 5 | underdeveloped |
| R+S(e) | 3 | 真尾e |
| P+R+R(e) | 2 | surface |
| P+P+R+R+S | 1 | international 类 |
| **合计** | **2304** | |

## 二、词根词族总表(词根 → 可拼出的全部表内词)

- **st 站立**:`P+P+R` arrest, exist, interest, misunderstand ｜ `P+P+R+S` existence, existent, misunderstanding ｜ `P+R` constant, distant, estate, install, instant, restore, understand, withstand ｜ `P+R+R+S` destination, instalation, restoration ｜ `P+R+S` bystander, circumstance, costly, distance, instal, installation, installment, instance, outstanding, substance, substantial, understanding ｜ `P+R+S+S` instalment, instantaneous ｜ `R+R` standpoint, staple, student ｜ `R+R+S` stabilise, stability, stabilize ｜ `R+S` stable, stage, standard, state, statement, station, statistic, status, stay ｜ `R+S(e)` stale, store ｜ `R+S+S` standardisation, standardise, standardization, standardize, stationary, statistical, storage
- **fac 做、造**:`P+P+R+S` reunification ｜ `P+R` affair, affect, benefit, defect, effect, infect, office, preface, profit, surface ｜ `P+R+R` deficit ｜ `P+R+S` affection, beneficial, beneficiary, defection, defective, deficiency, deficient, difficulty, effective, efficiency, efficient, infection, infectious, officer, official, proficiency, proficient, profitable, sufficient, superficial ｜ `P+R+S+S` affectionate ｜ `R+R+S` facility ｜ `R+S` facial, factor, factory, factual, faculty, fairy, fashion, feasibility, feasible ｜ `R+S+S` fashionable
- **per ?**:`P+P+R` experiment, persevere ｜ `P+P+R+R` imperialist ｜ `P+P+R+S` experimental, perseverance ｜ `P+R` perceive, percent, perfect, perform, persist, persuade ｜ `P+R+R+S` personality ｜ `P+R+S` emperor, experience, imperial, percentage, perception, perceptive, perfection, performance, performer, permissible, permission, permissive, persistence, persistent, perspective, persuasion, persuasive ｜ `P+R+S+S` imperialism ｜ `R+R` permit, person ｜ `R+R+S` permanence, permanent, personal ｜ `R+R+S+S` personally
- **vis 看**:`P+R` advise, devise, divide, interview, preview, review, revise, supervise ｜ `P+R+S` advisable, adviser, advisor, advisory, division, divisive, evidence, individual, interviewee, interviewer, invisible, reviewer, revision, supervision, supervisor, television ｜ `P+R+S+S` individualism ｜ `R+R` viewpoint, visit ｜ `R+R+S` visitor ｜ `R+S` visibility, visible, vision, visual ｜ `R+S+S` visionary, visualise, visualize
- **par 准备、出现、相等、生**:`P+P+R` disappear ｜ `P+P+R+S` disappearance ｜ `P+R` appear, compare, prepare, repair, separate ｜ `P+R+R` comparison, separatist ｜ `P+R+S` apparatus, apparent, appearance, comparable, comparative, preparation, preparatory, reparable, separation, transparency, transparent ｜ `P+R+S+S` apparently ｜ `R+R` parcel, pardon ｜ `R+R+S` particular ｜ `R+R+S+S` particularly ｜ `R+S` parent ｜ `R+S+S` parental, parenting
- **spect 看**:`P+R` aspect, inspect, prospect, respect ｜ `P+R+R+S` respectively ｜ `P+R+S` inspection, inspector, prospective, respectable, respectful, respective, suspicion, suspicious ｜ `P+R+S+S` especially ｜ `R+R` species, specific ｜ `R+R+S` specification ｜ `R+R+S+S` specifically ｜ `R+S` special, specify, speculate, speculation, speculative ｜ `R+S+S` specialisation, specialise, specialist, specialization, specialize
- **pon 放置**:`P+R` compose, compound, deposit, dispose, expose, impose, oppose, opposite, postpone, propose, suppose ｜ `P+R+S` component, composer, composition, disposal, exposure, imposing, imposition, opponent, opposition, postponement, preposition, proposal, proposition, supposition ｜ `R+S` position, positive
- **cap 拿、抓**:`P+R` accept, concept, deceit, deceive, except, receive ｜ `P+R+S` acceptable, acceptance, anticipate, anticipation, conceptual, deception, deceptive, exception, occupancy, occupant, occupation, occupational, occupy, receiver, reception, receptive, recipient ｜ `P+R+S+S` exceptional, receptionist ｜ `R+S` capture
- **plic 折、叠**:`P+P+R+S` exemplify, unemployment ｜ `P+R` apply, complex, comply, employ, imply, multiply, reply, supply ｜ `P+R+R` explicit, implicit ｜ `P+R+S` appliance, applicable, applicant, application, complexity, compliance, complicate, complication, employee, employer, employment, implication, multiplication, supplier
- **tend 伸展**:`P+R` attend, contend, content, extend, extent, intend, intense, intent, pretend, pretense ｜ `P+R+S` attendance, attendant, attention, attentive, contention, contentious, extension, extensive, intensify, intensity, intensive, intention, retention ｜ `P+R+S+S` intentional ｜ `R+S` tendency, tension
- **fer 带、承受**:`P+P+R+S` indifference, indifferent ｜ `P+R` differ, infer, interfere, offer, prefer, refer, suffer, transfer ｜ `P+R+R+S` inferiority ｜ `P+R+S` conference, difference, different, inference, inferior, interference, offering, preferable, preference, reference, suffering, transference ｜ `P+R+S+S` preferential ｜ `R+S` ferry
- **lig 绑、选**:`P+P+R` deadly, early, openly ｜ `P+P+R+R` analyst, analytic ｜ `P+P+R+R+S` analytical ｜ `P+R` oblige ｜ `P+R+R` deliver ｜ `P+R+R+S` delivery ｜ `P+R+S` alliance, diligence, diligent, intelligence, intelligent, obligation, reliable, reliance, religion, religious ｜ `R+R` liver ｜ `R+R+S` liability, livelihood, lively ｜ `R+S` liable
- **lect 收集、选择、读**:`P+R` allege, collect, college, dialect, elect, select ｜ `P+R+S` allegation, collection, collective, delegate, delegation, election, elective, elegance, elegant, illegal, intellectual, selection, selective ｜ `R+S` lecture, legal ｜ `R+S+S` legalise, legalize
- **vers 转**:`P+R` converse, convert, diverse, divorce, reverse, universe ｜ `P+R+S` advertise, controversial, controversy, conversation, conversion, convertible, diversify, diversity, reversal, reversible, universal, university ｜ `P+R+S+S` advertisement ｜ `R+S` version, versus, vertical
- **us 用**:`P+P+R` execute ｜ `P+P+R+S` beautiful, beautify, beauty, execution, executive ｜ `P+R` abuse, acute ｜ `P+R+S` abusive ｜ `R+S` usage, useful, useless, user, usual, utilisation, utilise, utility, utilization, utilize, virus ｜ `R+S+S` usually
- **duc 引导**:`P+P+R` reproduce ｜ `P+P+R+S` reproduction, reproductive ｜ `P+R` conduct, induce, introduce, produce, product, reduce ｜ `P+R+S` conductor, educate, education, educational, inducement, introduction, introductory, production, productive, productivity, reduction
- **sist 站立**:`P+R` assist, consist, constitute, insist, institute, resist, substitute ｜ `P+R+S` assistance, assistant, consistency, consistent, constitution, insistence, insistent, institution, resistance, resistant, substitution ｜ `P+R+S+S` constitutional, institutional
- **ven 来**:`P+R` avenue, event, invent, prevent, revenue ｜ `P+R+R+S` eventuality ｜ `P+R+S` adventure, convenience, convenient, convention, evening, eventful, eventual, invention, inventor, prevention, preventive ｜ `P+R+S+S` conventional, eventually ｜ `R+S` venture
- **ced 走**:`P+R` access, exceed, excess, proceed, process, succeed, success ｜ `P+R+S` accessible, ancestor, ancestry, excessive, procession, processor, recession, successful, succession, successive, successor ｜ `P+R+S+S` exceedingly
- **form 形状**:`P+R` conform, inform, reform, transform, uniform ｜ `P+R+R` reformist ｜ `P+R+S` conformity, informant, information, informative, reformation, transformation, uniformity ｜ `R+R+S` formality ｜ `R+S` formal, formation, former, formulate, formulation
- **mit 送、放**:`P+R` admit, commit, dismiss, emit, submit, transmit ｜ `P+R+S` admission, admittance, commission, commitment, committee, dismissal, emission, submission, submissive, transmission, transmitter ｜ `R+S` missing, mission
- **sid 坐**:`P+R` assess, beside, inside, outside, reside ｜ `P+R+R` besides, considerate ｜ `P+R+R+S` consideration ｜ `P+R+S` assessment, consider, insider, outsider, presidency ｜ `P+R+S+S` considerable, considering ｜ `R+S` residence, resident, session ｜ `R+S+S` residential
- **fin 界限**:`P+R` confine, define, refine ｜ `P+R+S` confinement, definite, definition, infinite, infinitive, infinity, refinement, refinery ｜ `P+R+S+S` definitely ｜ `R+S` final, finance, finish ｜ `R+S+S` finalise, finalize, finally
- **port 搬运**:`P+R` export, import, report, support, transport ｜ `P+R+S` exportation, importance, important, importation, proportion, reporter, supporter, supportive, transportation ｜ `P+R+S+S` proportional ｜ `R+S` portable, porter, portion
- **tain 握、保持**:`P+R` attain, contain, continue, obtain, retain, sustain ｜ `P+R+R+S` sustainability ｜ `P+R+S` attainment, container, containment, continent, continual, continuation, continuity, continuous, obtainment, sustainable ｜ `P+R+S+S` continental
- **tract 拉、拖**:`P+R` attract, contract, distract, retreat, subtract ｜ `P+R+S` attraction, attractive, contraction, contractor, distraction, subtraction ｜ `R+S` tractor, trailer, trainee, trainer, training, treatment, treaty
- **grad 步、级**:`P+P+R` disagree ｜ `P+P+R+S` disagreement ｜ `P+R` agree, congress, degree, progress, upgrade ｜ `P+R+S` agreeable, agreement, progression, progressive, undergraduate ｜ `P+R+S+S` congressional ｜ `R+S` gradual, graduate, graduation ｜ `R+S+S` gradually
- **sent 感觉**:`P+P+R` represent ｜ `P+P+R+S` representation, representative ｜ `P+R` absent, consent, nonsense, present, resent ｜ `P+R+S` consensus, presentation, presently, resentment ｜ `R+R+S` sensitivity ｜ `R+S` sensible, sensitive, sensor, sentence
- **press 压**:`P+R` depress, empress, express, impress ｜ `P+R+S` depression, expression, expressive, impression, impressive ｜ `R+R` president, pressure ｜ `R+R+S` presidential ｜ `R+S` presence, pressing, printer, printing
- **act 做**:`P+R` exact, interact, react ｜ `P+R+S` interaction, interactive, reaction, transaction ｜ `P+R+S+S` reactionary ｜ `R+S` acting, action, active, activity, actor, actual ｜ `R+S+S` actually
- **dic 说、指示**:`P+R` addict, contradict, predict ｜ `P+R+S` addiction, addictive, contradiction, contradictory, indicate, indication, indicative, predictable, prediction ｜ `R+S` dictate, dictation ｜ `R+S+S` dictionary
- **min 小、突出、仆**:`P+R+R` administrate ｜ `P+R+R+S` administration, administrative ｜ `P+R+S` administer, prominence, prominent ｜ `R+R(e)` minute ｜ `R+R+S` minority ｜ `R+S` mining, minister, ministry, minor, minus ｜ `R+S+S` mineral, ministerial
- **reg 统治、规则**:`P+R+S` irregular, irrigate, irrigation ｜ `R+R+S` regulator, regulatory ｜ `R+S` regard, region, register, regular, regulate, regulation ｜ `R+S+S` regarding, regardless, regional, regularity
- **pend 悬挂、衡量、支付**:`P+P+R+S` independence, indispensable ｜ `P+R` depend, expend, expense, suspend, suspense ｜ `P+R+S` compensate, compensation, expenditure, expensive, suspension ｜ `R+S` pension ｜ `R+S+S` pensioner
- **quer 寻求**:`P+R` acquire, conquer, conquest, enquire, inquire, request, require ｜ `P+R+S` acquisition, conqueror, enquiry, inquiry, inquisitive, requirement ｜ `R+S` question
- **serv 服务、保持**:`P+R` deserve, observe, preserve, reserve ｜ `P+R+S` conservative, observant, observation, observer, preservation, preservative ｜ `R+S` reservation, servant, service, serving
- **val 价值、强**:`P+R` interval, prevail ｜ `P+R+R+S` availability ｜ `P+R+S` available, evaluate, evaluation, invalid, invaluable, prevalence, prevalent ｜ `R+R+S` validity ｜ `R+S` valid, valuable ｜ `R+S+S` validate
- **cord 心**:`P+R` accord, record ｜ `P+R+R+S` encouraging ｜ `P+R+S` accordance, discourage, encourage, recorder, recording ｜ `P+R+S+S` accordingly, encouragement ｜ `R+R+S` courageous ｜ `R+S` courage ｜ `R+S+S` courtyard
- **gen 出生、产生**:`R+R` generate, genetic ｜ `R+R+S` generator, generosity ｜ `R+R+S+S` genetically ｜ `R+S` general, generation, generous, genius, genuine ｜ `R+S+S` generalise, generalize, generally
- **man 手**:`P+R` postman, remain ｜ `R+R` maintain ｜ `R+R+S` maintenance, manager, manufacture ｜ `R+R+S+S` managerial ｜ `R+S` manage, manhood, manly, manual, many ｜ `R+S+S` management
- **mon 警告**:`P+R` common ｜ `P+R+R` commonplace ｜ `P+R+S` demonstrate, demonstration, demonstrative ｜ `R+S` money, monitor, monster, monstrous, month, monument ｜ `R+S+S` monthly, monumental
- **not 标记、名字**:`P+R` announce, pronoun, pronounce ｜ `P+R+S` announcement, pronunciation ｜ `R+R+S` notification ｜ `R+S` notable, notation, notice, notify, notion ｜ `R+S+S` noticeable, notional
- **part 部分**:`P+R` apart, counterpart, depart ｜ `P+R+S` apartment, department, departure ｜ `R+R+S` participant, participate, participation ｜ `R+S` partial, partly, party ｜ `R+S+S` partially
- **spond 承诺、回答**:`P+P+R` correspond ｜ `P+P+R+S` correspondence, correspondent, corresponding ｜ `P+R` respond, response ｜ `P+R+S` respondent, responsibility, responsible, responsive ｜ `R+S` sponsor ｜ `R+S+S` sponsorship, spontaneous
- **hab 持有**:`P+R` behave, exhibit, inhabit, prohibit ｜ `P+R+S` behavior, exhibition, inhabitant, prohibition, prohibitive ｜ `P+R+S+S` behavioral ｜ `R+R` habit ｜ `R+S` habitual
- **mot 动**:`P+R` promote, remote, remove ｜ `P+R+S` automobile, emotion, promotion ｜ `P+R+S+S` emotional ｜ `R+S` mobile, motion, motive, motor, movement
- **popul 人民**:`P+R+S` republic ｜ `P+R+S+S` republican ｜ `R+R+S` publicity ｜ `R+S` populate, population, populous, public, publish ｜ `R+S+S` publication, publicise, publicize, publicly
- **struct 建造**:`P+R` construct, instruct ｜ `P+R+S` construction, destruction, destructive, infrastructure, instruction, instructive, instructor, instrument ｜ `P+R+S+S` instrumental ｜ `R+S` structure
- **ver 真实**:`P+P+R` discover, recover, uncover ｜ `P+P+R+S` discovery, recovery ｜ `P+R` cover, severe ｜ `P+R+S` coverage, severity ｜ `R+R+S` verification ｜ `R+S` verify, very
- **vit 生命**:`P+P+R+S` inevitable ｜ `P+R` invite, survive ｜ `P+R+S` invitation, inviting, survival, survivor ｜ `R+R` vitamin ｜ `R+R+S` vitality ｜ `R+S` vigor, vital ｜ `R+S+S` vigorous
- **her 粘**:`P+P+R` preacher ｜ `P+R+S` coherence, coherent, cohesion, cohesive ｜ `R+R+S` hesitant, hesitate, hesitation ｜ `R+S` heroic, heroine, heroism
- **jus 法律、正**:`P+R` adjust, injure ｜ `P+R+S` adjustment, injurious, injury, prejudice ｜ `R+R+S` justification ｜ `R+S` juror, jury, justice, justify
- **prov 试验、证明**:`P+P+R` disapprove ｜ `P+P+R+S` disapproval ｜ `P+R` approve, improve ｜ `P+R+S` approval, improvement ｜ `R+R+S` probability ｜ `R+S` probable, probably, provide, provision
- **soci 同伴**:`P+R+S` associate, association ｜ `R+R+S` sociologist ｜ `R+S` sociable, social, society, sociology ｜ `R+S+S` socialise, socialism, socialist, socialize
- **clud 关**:`P+R` conclude, exclude, include ｜ `P+R+S` conclusion, conclusive, exclusion, exclusive, including, inclusion, inclusive
- **cre ?**:`P+R` decrease, increase, recruit ｜ `P+R+S` recreation, recreational, recruitment ｜ `P+R+S+S` increasingly ｜ `R+S` create, creation, creative
- **fend 打击**:`P+R` defence, defend, defense, offence, offend, offense ｜ `P+R+S` defendant, defensive, offender, offensive
- **it 走**:`P+P+R` reunite ｜ `P+R+R` either ｜ `P+R+S` ambition, ambitious, initial, initiate, initiation, initiative, unity ｜ `P+R+S+S` initially
- **mat 母、熟**:`P+R` automate ｜ `P+R+S` automatic, automation ｜ `P+R+S+S` automatically ｜ `R+S` material, mature ｜ `R+S+S` materialise, materialism, materialist, materialize
- **mod 方式、适度**:`P+P+R+S` accommodate, accommodation ｜ `P+R+S` commodity ｜ `R+R` moderate, modest ｜ `R+R+S` moderation, moderator, modesty, modification ｜ `R+S` modify
- **pet 追求**:`P+R` appetite, compete ｜ `P+R+S` competence, competent, competition, competitive, competitor, repetition, repetitive ｜ `R+S` petty
- **ple 满**:`P+R` complete, multiple, triple ｜ `P+R+S` completion, implement, supplement ｜ `P+R+S+S` implementation, supplementary ｜ `R+R` pleasure ｜ `R+S` plenty
- **pol 城**:`R+R` policeman ｜ `R+R+S` pollution ｜ `R+S` police, policy, polish, polite, politeness, political, politician, politics
- **pot ?**:`P+R+S` impossibility, impossible ｜ `R+S` possess, possibility, possible, power ｜ `R+S+S` possession, possessive, potential, powerful
- **put 计算、思考**:`P+R` compute, dispute, input, output ｜ `P+R+S` deputy, disputable ｜ `R+S` computation, computer ｜ `R+S+S` computerise, computerize
- **sum 拿、总**:`P+R` assume, consume, resume ｜ `P+R+S` assumption, consumer, consumption, resumption ｜ `R+R` summit ｜ `R+R+S` summarize, summary
- **vi 道路**:`P+R` convey, survey ｜ `P+R+R` evident ｜ `P+R+S` conveyance, envious, obvious, previous ｜ `P+R+S+S` obviously ｜ `R+R` vivid ｜ `R+S` voyage
- **volv 转、卷**:`P+R` evolve, involve, revolve ｜ `P+R+S` evolution, involvement, revolution ｜ `P+R+S+S` evolutionary, revolutionary ｜ `R+R+S` voluminous ｜ `R+S` voltage
- **ag 做**:`P+P+R` coinage ｜ `P+R` image ｜ `P+R+S` aggression, aggressive ｜ `R+R+S` teenager ｜ `R+S` ageing, agency, agent, aging
- **count ?**:`P+R` account, discount ｜ `P+R+R+S` accountability ｜ `P+R+S` accountable, accountancy, accountant, accounting, encounter ｜ `R+S` counter
- **dit 给**:`P+R` edit ｜ `P+R+R+S` editorial ｜ `P+R+S` addition, condition, edition, editor ｜ `P+R+S+S` additional, conditional, conditioner
- **geo 地**:`R+R+S` geographic, geography, geological, geometric, geometry ｜ `R+R+S+S` geographically, geologically, geometrically ｜ `R+S` geology
- **ject 投掷**:`P+R` object, reject, subject ｜ `P+R+S` adjective, objection, objective, projection, rejection, subjective
- **nat 出生**:`P+R` senate ｜ `P+R+R(e)` discriminate ｜ `P+R+R+S` discrimination, discriminatory ｜ `P+R+S+S` international ｜ `R+S` nation, native, nature ｜ `R+S+S` national
- **or 口、说**:`P+P+R` interior, superior ｜ `P+R+S` colorful, corridor ｜ `R+R` orphan ｜ `R+R+S` orphanage ｜ `R+S` major, oral, vendor
- **sign 记号**:`P+R` assign, design, resign ｜ `P+R+S` assignment, designer ｜ `R+R+S` significance, significant ｜ `R+S` resignation, signal
- **sol 太阳、唯一**:`P+R+S` isolate, isolation ｜ `P+R+S+S` consolidate, consolidation ｜ `R+R` solvent ｜ `R+R+S` solidity ｜ `R+S` solely, solid ｜ `R+S+S` solidify
- **stinct 刺、区分**:`P+P+R` existing, interesting ｜ `P+R` distinct, instinct ｜ `P+R+S` distinction, distinctive, distinguish, instinctive ｜ `P+R+S+S` distinguishable
- **strain 拉紧**:`P+R` distress, district, restrain, restraint, restrict ｜ `P+R+S` distressful, restriction, restrictive ｜ `R+S` stressful
- **tact 接触**:`P+R` attach, attack, contact, intact ｜ `P+R+R(e)` contaminate ｜ `P+R+R+S` contamination ｜ `P+R+S` attachment, intangible ｜ `R+S` tangible
- **aug 增大**:`R+R` august ｜ `R+R+S` authorisation, authority ｜ `R+S` author ｜ `R+S+S` authentic, authorise, authorization, authorize
- **cent 百**:`P+R` accent, decent ｜ `P+R+R` concentrate ｜ `P+R+S` incentive ｜ `R+R` centimeter ｜ `R+R(e)` centigrade, centimetre ｜ `R+S` center
- **cid2 ?**:`P+R` acid, decide ｜ `P+R+S` accident, acidity, incidence, incident ｜ `P+R+S+S` accidental, incidental
- **flu 流**:`P+R+S` affluence, affluent, influence, influenza ｜ `P+R+S+S` influential ｜ `R+S` fluency, fluent, fluid
- **fort 强**:`P+P+R` reinforce ｜ `P+P+R+S` reinforcement ｜ `P+R` comfort, effort, enforce ｜ `P+R+S` comfortable, enforcement ｜ `R+S` forceful
- **ign ?**:`P+R` ignore ｜ `P+R+S` ignorance, ignorant, pregnancy, pregnant, recognise, recognition, recognize
- **lat 携带**:`P+R` relate, translate ｜ `P+R+S` relation, relative, relativity, translation, translator ｜ `P+R+S+S` relationship
- **log 说话、学科**:`P+R` dialog, dialogue ｜ `P+R+R` apologetic ｜ `P+R+S` apologise, apologize, apology ｜ `R+S` logic, logical
- **ord 秩序**:`P+R+R+S` coordinator ｜ `P+R+S` coordinate, coordination, disorder, extraordinary ｜ `R+S` order, ordinary ｜ `R+S+S` orderly
- **organ 器官、工具**:`R+S` organic, organisation, organise, organism, organization, organize ｜ `R+S+S` organisational, organizational
- **pan 满、展**:`P+P+R+S` accompany ｜ `P+R` expand ｜ `P+R+S` companion, company, expansion, expansive ｜ `P+R+S+S` companionship ｜ `R+S` panic
- **punct 点、刺**:`P+P+R` disappoint ｜ `P+P+R+S` disappointing, disappointment ｜ `P+R` appoint ｜ `P+R+S` appointment ｜ `R+R+S` punctuality ｜ `R+S` pointless, punctual
- **real ?**:`R+S` realisation, realise, realism, realistic, reality, realization, realize, really
- **riv 河、源**:`P+R` arrive, derive ｜ `P+R+S` arrival, derivation, derivative ｜ `R+S` rival, river ｜ `R+S+S` rivalry
- **simil 相似**:`P+R` assemble, resemble ｜ `P+R+S` assembly, resemblance ｜ `R+R` simple, simply ｜ `R+R+S` simplicity, simplify
- **terr 地、怕**:`R+R` terrific ｜ `R+R+S` territory ｜ `R+R+S+S` territorial ｜ `R+S` terrible, terrify, terror ｜ `R+S+S` terrorism, terrorist
- **vel 盖、卷**:`P+P+R+R` underdeveloped ｜ `P+R` develop, envelope, reveal ｜ `P+R+R` developer ｜ `P+R+S` development, revelation ｜ `P+R+S+S` developmental
- **bio 生命**:`R+R+S` biographer, biographical, biography, biological, biologist, biotechnology ｜ `R+S` biology
- **clar 清楚**:`P+R` declare ｜ `P+R+S` declaration, declarative ｜ `R+R+S` clarification ｜ `R+S` clarify, clarity, clearly
- **cur 关心**:`P+R` occur, secure ｜ `P+R+S` accuracy, accurate, security ｜ `R+S` curable, curious
- **gest 带来**:`P+R` digest, suggest ｜ `P+R+S` digestion, digestive, suggestion, suggestive ｜ `R+S` gesture
- **hospit 客、敌**:`R+R+S` hospitality, hostility ｜ `R+S` hospitable, hospital, hostess, hostile ｜ `R+S+S` hospitalize
- **imag 像、模仿**:`R+R+S` imagination, imaginative ｜ `R+S` imagine, imaging, imitate, imitation, imitative
- **liber 自由**:`P+R+S` deliberate, deliberation ｜ `R+S` liberal, liberate, liberation, liberty, library
- **mand 命令、托付**:`P+P+R` recommend ｜ `P+P+R+S` recommendation ｜ `P+R` command, demand ｜ `P+R+S` commander, demanding ｜ `R+R+S` tremendous
- **merc 交易**:`P+R` commerce ｜ `P+R+S` commercial ｜ `P+R+S+S` commercialise, commercialize ｜ `R+S` merchant, merciful, mercy
- **norm 规范**:`P+R+S` enormous ｜ `R+S` normal ｜ `R+S+S` normalisation, normalise, normalization, normalize, normally
- **opt 选择、最好**:`P+R` adopt ｜ `P+R+S` adoption ｜ `R+R` optimist ｜ `R+S` optimism, optimistic, option ｜ `R+S+S` optional
- **pass 步**:`P+R` bypass, compass ｜ `R+R` passport ｜ `R+S` passage, passion, passive ｜ `R+S+S` passionate
- **pen 惩罚**:`P+P+R+R` independent ｜ `P+R+R` dependent ｜ `P+R+R+S` compensatory ｜ `R+R+S` penalty, penetration ｜ `R+S+S` penalise, penalize
- **pla 平**:`P+P+R+S` exemplary ｜ `P+R` replace ｜ `P+R+R+S` explanatory ｜ `P+R+S` display, replacement ｜ `R+R` platform ｜ `R+S` placement
- **pri 在前**:`R+R` priest, prison ｜ `R+R+S` priceless, primarily, priority, prisoner ｜ `R+S` prior
- **rupt 破裂**:`P+R` corrupt, disrupt, interrupt ｜ `P+R+S` corruption, disruption, disruptive, interruption
- **sult ?**:`P+R` consult, insult, result ｜ `P+R+S` consultant, consultation, consultative, resultant
- **urb 城市**:`P+R` suburb ｜ `P+R+S` suburban ｜ `R+S` urban ｜ `R+S+S` urbanisation, urbanise, urbanization, urbanize
- **var 变化**:`P+R+S` invariably ｜ `R+S` variable, variant, variation, variety, various, vary
- **voc 呼唤**:`P+R` provoke ｜ `P+R+S` advocacy, advocate, provocation, provocative ｜ `R+S` vocation, vocational
- **cit 唤起、引**:`P+R` excite, recite ｜ `P+R+S` excitement, exciting, recitation ｜ `R+S` citation
- **commun 公共**:`R+R` communist ｜ `R+S` communism, community ｜ `R+S+S` communicate, communication, communicative
- **crit 判断**:`R+S` critic, critical ｜ `R+S+S` criterion, criticise, criticism, criticize
- **cult 耕种、培养**:`P+R+S` accumulate, accumulation, accumulative ｜ `R+S` cultivate, cultivation, culture
- **dur 持续**:`P+R` endure ｜ `P+R+S` endurance ｜ `R+R+S` durability ｜ `R+S` durable, duration, during
- **fund 底、基础**:`P+R` profound ｜ `R+S` foundation, founder, founding, funding ｜ `R+S+S` fundamental
- **hon 荣誉**:`R+R` honest ｜ `R+R+S` honesty, honorary ｜ `R+S` honey, honor ｜ `R+S+S` honorable
- **medi 中间**:`P+R+S` immediate, intermediary, intermediate, remedial ｜ `P+R+S+S` immediately ｜ `R+S` medium
- **metr 测量**:`P+R` diameter, kilometer, kilometre, millimeter, millimetre ｜ `R+S` metric
- **migr 迁移**:`P+R+S` immigrant, immigrate, immigration ｜ `R+S` migrant, migrate, migration
- **pel 推、驱**:`P+R` compel, expel, impulse ｜ `P+R+S` compelling, compulsory, impulsive
- **qual 种类、质**:`P+R` equal ｜ `P+R+S` equally ｜ `R+R+S` qualification, qualitative ｜ `R+S` qualify, quality
- **rect 直、正**:`P+R` correct, direct ｜ `P+R+S` correction, corrective, direction, director
- **scrib 写**:`P+R` describe, prescribe ｜ `P+R+S` description, descriptive, prescription ｜ `R+S` scripture
- **sequ 跟随**:`P+R+S` consequence, consequent, subsequent ｜ `P+R+S+S` consequently ｜ `R+S` sequence ｜ `R+S+S` sequential
- **solv 松开、解决**:`P+R` absolute, dissolve, resolute, resolve ｜ `P+R+S` resolution ｜ `R+S` solution
- **tect 覆盖**:`P+R` detect, protect ｜ `P+R+S` detection, detective, protection, protective
- **tra ?**:`P+R+R` contrast ｜ `P+R+S` betray, contrary ｜ `P+R+S+S` betrayal ｜ `R+R+S` tradition ｜ `R+R+S+S` traditional
- **van 前、空**:`P+R` advance ｜ `P+R+R+S` advantageous ｜ `P+R+S` advancement, advantage ｜ `R+S` vanish, vanity
- **vinc 征服**:`P+R` convince, province ｜ `P+R+S` convincing, provincial ｜ `R+R+S` victorious ｜ `R+S` victory
- **arch 首、主要**:`P+P+R` research ｜ `P+P+R+S` researcher ｜ `P+R` search ｜ `R+R` architect ｜ `R+R+S` architecture
- **bank2 凳**:`R+R` bankrupt ｜ `R+R(e)` banknote ｜ `R+R+S` bankruptcy ｜ `R+S` banker, banking
- **bat 打**:`P+R` combat, debate ｜ `P+R+S` combative ｜ `R+S` battery ｜ `R+S+S` battalion
- **camp 田、营**:`R+R` campaign ｜ `R+R(e)` champagne ｜ `R+S` campus, champion ｜ `R+S+S` championship
- **caus 原因、诉**:`P+R` accuse, because, excuse ｜ `P+R+S` accusation, discussion
- **circ ?**:`R+R` circuit ｜ `R+S` circular, circulate, circulation, circus
- **cis 切**:`P+R` precise ｜ `P+R+S` decision, decisive, precisely, precision
- **clin 倾**:`P+R` decline, incline ｜ `P+R+S` inclination ｜ `R+S` clinic, clinical
- **colon 垦殖**:`R+S` colonial, colonise, colonize, colony ｜ `R+S+S` colonialism
- **cret ?**:`P+R` concrete, secret ｜ `P+R+S` secretary, secretive ｜ `R+S` secrecy
- **curr 跑**:`P+R+S` excursion, occurrence ｜ `R+S` currency, current ｜ `R+S+S` currently
- **doc 教**:`R+R` doctorate ｜ `R+S` doctor, document ｜ `R+S+S` doctoral, documentary
- **fess 说**:`P+R` confess ｜ `P+R+S` confession, profession, professor ｜ `P+R+S+S` professional
- **flect 弯曲**:`P+R` reflect ｜ `P+R+S` reflection, reflective ｜ `R+S` flexibility, flexible
- **gram 字母、写**:`P+R` diagram, kilogram, program ｜ `R+R` grammar ｜ `R+R+S` grammatical
- **grat 高兴、感谢**:`P+R+R+S` congratulatory ｜ `P+R+S` congratulate, congratulation ｜ `R+S` grateful, gratitude
- **integr 完整**:`P+R+S` disintegrate, disintegration ｜ `R+S` integrate, integration, integrity
- **lim 界限**:`P+R+S` eliminate, elimination, preliminary ｜ `R+R` limit ｜ `R+R+S` limitation
- **liter 文字**:`R+S` literacy, literal, literary, literate ｜ `R+S+S` literally
- **mar 海、丈夫**:`R+R` marital, marvel ｜ `R+R+S` marvelous ｜ `R+S` marine, marry
- **ment 心智**:`P+R` comment ｜ `P+R+S` commentary ｜ `R+R+S` mentality ｜ `R+S` mental, mention
- **nov 新**:`P+R+S` innovate, innovation, innovative, renovate, renovation
- **ori 升起、源头**:`R+R(e)` originate ｜ `R+R+S` majority ｜ `R+S` orient ｜ `R+S+S` oriental, orientation
- **pac 平、付**:`P+R` repay ｜ `P+R+S` repayment ｜ `R+R` payroll ｜ `R+S` payment, peaceful
- **pract 实践**:`R+S` practical, practice, practise ｜ `R+S+S` practicable, practicality
- **prehens 抓**:`P+R` comprehend, comprise, surprise ｜ `P+R+S` comprehension, comprehensive
- **priv 个人、夺**:`P+R` deprive ｜ `P+R+S` deprivation ｜ `R+R(e)` privilege ｜ `R+S` privacy, private
- **sci 知**:`P+R+S` conscience, conscious ｜ `P+R+S+S` conscientious ｜ `R+S` science ｜ `R+S+S` scientist
- **sect 切、分**:`P+R` insect ｜ `R+S` section, sector, segment ｜ `R+S+S` segmental
- **spir 呼吸**:`P+R` inspire ｜ `P+R+S` inspiration, inspirational ｜ `R+R` spirit ｜ `R+R+S` spiritual
- **tir ?**:`P+R` entire, retire ｜ `P+R+S` entirety, retirement ｜ `R+S` tiring
- **vest 穿衣、脚印**:`P+R` invest ｜ `P+R+S` investigate, investigation, investment, investor
- **apt 适合**:`P+R` adapt ｜ `P+R+S` adaptation, adaptive ｜ `R+S` aptitude
- **art 技艺**:`R+R` artist ｜ `R+R+S` artificial ｜ `R+R+S+S` artistically ｜ `R+S` artistic
- **astr 星**:`P+R` disaster ｜ `P+R+S` disastrous ｜ `R+R` astronaut, astronomy
- **cel 高、藏**:`P+R` conceal, excel ｜ `P+R+R` accelerate ｜ `P+R+S` concealment
- **centr 中心**:`P+R+S` concentration ｜ `R+S` central ｜ `R+S+S` centralise, centralize
- **cert 确定**:`P+R` concert ｜ `P+R+S` uncertainty ｜ `R+R+S` certificate ｜ `R+S` certainty
- **claim 喊**:`P+R` exclaim, proclaim ｜ `P+R+S` exclamation, proclamation
- **class 等级**:`R+R+S` classification ｜ `R+S` classic, classical, classify
- **dem 人民**:`P+P+R+S` academic ｜ `R+R` democracy, democrat ｜ `R+R+S` democratic
- **domin 统治**:`R+S` dominance, dominant, dominate, domination
- **don 给**:`P+P+R` abandon ｜ `R+S` donate, donation, donor
- **electr 电**:`R+R+S` electricity ｜ `R+S` electric, electrical, electrician
- **equ 相等**:`P+R+S` adequacy, equivalence, equivalent ｜ `R+R+S` equality
- **ess 存在**:`P+R` obese ｜ `P+R+S` obesity ｜ `R+R+S` essential ｜ `R+S` essence
- **fat 说、命运**:`R+R` father ｜ `R+R+S` fatality, fatherly ｜ `R+S` fatal
- **frag 破**:`R+S` fraction, fragment ｜ `R+S+S` fragmentary, fragmentation
- **fus 流、倒**:`P+R` confuse, refuse ｜ `P+R+S` confusion, refusal
- **herit 继承**:`P+R` inherit ｜ `P+R+S` inheritance ｜ `R+S` heiress, heritage
- **hum 湿土**:`R+S` human, humor ｜ `R+S+S` humanity, humorous
- **ident ?**:`R+R+S` identification ｜ `R+S` identical, identify, identity
- **labor 劳动**:`P+R+S` collaborate, collaboration, collaborative ｜ `R+S` laborious
- **lev 举、轻**:`P+R+S` elevate, elevation, relevance, relevant
- **loc 地方**:`R+R+S` locality ｜ `R+S` local, locate, location
- **memor 记忆**:`R+S` memorial, memorise, memorize, memory
- **mer 值得**:`R+R` merit ｜ `R+R+S+S` meritorious ｜ `R+S` merely, merry
- **neutr 两者都不**:`R+R+S` neutrality ｜ `R+S` neutral ｜ `R+S+S` neutralise, neutralize
- **oper 工作**:`P+R+S` cooperate, cooperation, cooperative ｜ `R+S` operate
- **path 感受**:`P+R+R` sympathetic ｜ `P+R+S` sympathise, sympathize, sympathy
- **pati 忍受**:`P+R+S` impatience, impatient ｜ `R+S` patience, patient
- **pect 看**:`P+R` expect, suspect ｜ `P+R+S` expectancy, expectation
- **phan 显现**:`P+R` infant ｜ `P+R+S` emphasise, emphasize ｜ `R+R+S` fantastic
- **photo 光**:`R+R` photograph ｜ `R+R+S` photographer, photographic, photography
- **phys 自然、身体**:`R+S` physical, physician, physics ｜ `R+S+S` physicist
- **pret ?**:`P+R` interpret ｜ `P+R+S` interpretation, interpreter ｜ `R+S` pretence
- **prox 接近**:`P+R` approach ｜ `P+R+S` approximate, approximation ｜ `P+R+S+S` approximately
- **pur ?**:`R+R(e)` purpose ｜ `R+R+S` purposeful ｜ `R+S` purify, purity
- **quant 量**:`R+R+S` quantification, quantitative ｜ `R+S` quantify, quantity
- **quart 四**:`P+P+R` adequate ｜ `P+P+R+S` adequately ｜ `R+S` quarter ｜ `R+S+S` quarterly
- **scop 看、镜**:`P+R` microscope, telescope ｜ `P+R+S` microscopic, telescopic
- **sert 缚、放**:`P+R` desert, insert ｜ `P+R+S` desertion, insertion
- **sure 省心、确信**:`P+R` assure, ensure, insure ｜ `R+R` treasure
- **techn 技艺**:`R+R+S` technological ｜ `R+S` technical, technician, technology
- **the 神、观、放**:`R+R` theatre ｜ `R+R+S` therapist, therapy ｜ `R+S` theory
- **toler 忍受**:`R+S` tolerable, tolerance, tolerant, tolerate
- **tort 扭**:`P+R+S` attorney ｜ `R+R` tourist ｜ `R+S` torture, tourism
- **und 波**:`P+R` around ｜ `P+R+S` abundance, abundant ｜ `R+S` under
- **vac ?**:`R+S` vacancy, vacant, vacation, vacuum
- **vad 走、入侵**:`P+R` invade ｜ `P+R+S` invader, invasion, invasive
- **verb 词语**:`P+R` adverb, proverb ｜ `P+R+S` proverbial ｜ `R+S` verbal
- **viol ?**:`R+S` violate, violation, violence, violent
- **alt 高**:`R+R+S` alteration ｜ `R+S` alter, altitude
- **cad 落**:`P+R+S` occasion ｜ `P+R+S+S` occasional ｜ `R+S` casual
- **capit 头**:`R+S` capital ｜ `R+S+S` capitalism, capitalist
- **cred ?**:`P+R+S` incredible ｜ `R+R` credit ｜ `R+R+S` creditable
- **eco 家、环境**:`R+R` economy ｜ `R+R+S` ecological ｜ `R+S` ecology
- **estim 估价**:`P+R+S` underestimate ｜ `R+S` estimate, estimation
- **fam ?**:`R+S` family, famine, famous
- **fid 信**:`P+R+S` confidence, confident ｜ `P+R+S+S` confidential
- **horr 害怕**:`R+S` horrible, horrify, horror
- **ide ?**:`R+S` ideal ｜ `R+S+S` idealism, idealistic
- **lic 允许**:`P+R+S` delicate, delicious ｜ `R+S` licence
- **lustr ?**:`R+S` illustrate, illustration, illustrative
- **magn ?**:`R+R` magnetic ｜ `R+R+S` magnificence, magnificent
- **med 治疗**:`P+R+S` remedy ｜ `R+S` medical ｜ `R+S+S` medicine
- **merg 沉**:`P+R` emerge ｜ `P+R+S` emergence, emergency
- **mir 惊奇**:`P+R` admire ｜ `P+R+S` admirable, admiration
- **mix 混合**:`P+P+R` seeming ｜ `P+P+R+S` seemingly ｜ `R+S` mixer
- **mut ?**:`P+R` commute ｜ `P+R+S` commuter ｜ `R+S` mutual
- **narr 讲述**:`R+S` narrate, narration, narrative
- **nect 绑**:`P+R` connect ｜ `P+R+S` connection, connexion
- **plod 拍、爆**:`P+R` explode ｜ `P+R+S` explosion, explosive
- **prim 第一**:`R+R+S` principal ｜ `R+S` primary, primitive
- **psych 心灵**:`R+R+S` psychological, psychologist ｜ `R+S` psychology
- **rat 算、推定**:`P+R+S+S` irrational ｜ `R+S` rating ｜ `R+S+S` rational
- **rem ?**:`P+R` supreme ｜ `P+R+S` supremacy ｜ `R+R` removal
- **rend 给**:`P+R+S` surrender ｜ `R+S` render ｜ `R+S+S` rendering
- **roll 轮、卷**:`P+R` enroll ｜ `P+R+S` enrollment ｜ `R+S` roller
- **sat 满**:`R+R+S` satisfaction, satisfactory ｜ `R+S` satisfy
- **scend 攀**:`P+R` descend, descent ｜ `P+R+S` descendant
- **sil 静**:`P+R` missile ｜ `R+S` silence, silent
- **tempt 轻视、试**:`P+R` attempt, contempt ｜ `P+R+S` contemptible
- **termin 界限**:`P+R` determine ｜ `P+R+S` determination ｜ `R+S` terminal
- **test 见证**:`P+R` contest, protest ｜ `P+R+S` contestant
- **text 编织**:`P+R` context ｜ `P+R+S` contextual ｜ `R+S` textile
- **trud 推、冲**:`P+R` intrude ｜ `P+R+S` intruder, intrusion
- **virt ?**:`R+S` virtual, virtuous ｜ `R+S+S` virtually
- **vol 意愿**:`R+S` voluntary, volunteer ｜ `R+S(e)` volume
- **vot 誓、愿**:`P+R` devote ｜ `P+R+S` devotion ｜ `R+S` voter
- **abil 能**:`P+R+S` disability ｜ `R+S` ability
- **ann 年**:`R+R+S` anniversary ｜ `R+S` annual
- **audi 听**:`R+R+S` auditorium ｜ `R+S` audience
- **bell 战争**:`P+R+S` rebellion, rebellious
- **cand ?**:`R+S+S` candidacy, candidate
- **celebr 庆贺**:`R+S` celebrate, celebration
- **cern ?**:`P+R` concern ｜ `P+R+S` concerning
- **chiev 头**:`P+R` achieve ｜ `P+R+S` achievement
- **corp 身体**:`R+R` corporate ｜ `R+S` corporation
- **demn 罚**:`P+R` condemn ｜ `P+R+S` condemnation
- **dent 牙**:`R+R` dentist ｜ `R+S` dental
- **dign 值得**:`R+S` dignify, dignity
- **diplo 双重**:`R+R` diplomat ｜ `R+R+S` diplomatic
- **dynam 力**:`R+S` dynamic, dynamics
- **feder 联盟**:`R+S` federal, federation
- **fict ?**:`R+S` fiction ｜ `R+S+S` fictional
- **fig ?**:`R+R+S` figurative ｜ `R+S` figure
- **firm 坚固**:`P+R` confirm ｜ `P+R+S` confirmation
- **frequ 频繁**:`R+S` frequency, frequent
- **frig 冷**:`P+R+R+S` refrigeration, refrigerator
- **funct 做、起作用**:`R+S` function ｜ `R+S+S` functional
- **graph 写、画**:`R+S` graphic ｜ `R+S+S` graphically
- **grav 重**:`R+R+S` gravitation ｜ `R+S` gravity
- **lax ?**:`P+R` relax ｜ `P+R+S` relaxation
- **leis 许可、闲**:`R+S` leisure ｜ `R+S+S` leisurely
- **long 长**:`R+S` length ｜ `R+S+S` lengthy
- **lus 玩**:`P+R+S` illusion, illusory
- **meas 量**:`R+S` measure ｜ `R+S+S` measurement
- **mor 习俗、道德**:`R+R+S` morality ｜ `R+S` moral
- **nav 船**:`R+S` naval, navy
- **noc ?**:`P+R+S` innocence, innocent
- **nutr 滋养**:`R+S` nutrient, nutrition
- **orb 圆、轨**:`R+R` orbit, orbital
- **oxy 酸、氧**:`P+R+S` dioxide ｜ `R+R` oxygen
- **pact ?**:`P+R` compact, impact
- **phon 声音**:`P+R` microphone, telephone
- **plaud ?**:`P+R` applaud, applause
- **pleas 高兴**:`R+S` pleasant, pleasing
- **plor 哭、喊**:`P+R` explore ｜ `P+R+S` exploration
- **pun 罚**:`R+S` punish ｜ `R+S+S` punishment
- **quaint ?**:`P+R` acquaint ｜ `P+R+S` acquaintance
- **rap ?**:`R+R+S` rapidity ｜ `R+S` rapid
- **ras ?**:`P+R` erase ｜ `P+R+S` eraser
- **sacr ?**:`R+R(e)` sacrifice ｜ `R+R+S` sacrificial
- **sal 盐**:`R+S` salary, salty
- **san 健康**:`P+R` insane ｜ `P+R+S` insanity
- **sen 老**:`R+R+S` seniority ｜ `R+S` senior
- **sorb 吸收**:`P+R` absorb ｜ `P+R+S` absorption
- **sper 希望**:`P+R` despair ｜ `P+R+S` desperate
- **tempor 时间、调和**:`P+R+S` contemporary ｜ `R+S` temporary
- **tom 切**:`P+R` atom ｜ `P+R+S` atomic
- **ton ?**:`P+R+S` astonish ｜ `P+R+S+S` astonishment
- **trem ?**:`P+R` extreme ｜ `P+R+S` extremity
- **tribu ?**:`P+R+S` contribution, distribution
- **turb 搅乱**:`P+R` disturb ｜ `P+R+S` disturbance
- **typ 模型**:`R+R` typist ｜ `R+S` typical
- **ultim 最后**:`R+S` ultimate ｜ `R+S+S` ultimately
- **urg ?**:`R+S` urgency, urgent
- **zo 动物**:`R+R+S` zoological ｜ `R+S` zoology
- **agri 田**:`R+R+S` agriculture
- **ambul 行走**:`R+S` ambulance
- **avi 鸟**:`R+S` aviation
- **bar 杆**:`R+S` barrier
- **celer 快**:`P+R+S` acceleration
- **civ 市民**:`R+R+S` civilian
- **cosm ?**:`R+S` cosmic
- **cruc ?**:`R+S` crucial
- **decor 雅、装饰**:`R+S` decorate
- **dorm 眠**:`R+R+S` dormitory
- **err 错**:`R+S` error
- **fix 固定**:`R+S` fixation
- **flor 花**:`R+S` flourish
- **fut ?**:`R+S` future
- **hydr ?**:`P+R` hydrogen
- **junct 连接**:`P+R+S` conjunction
- **langu 舌**:`R+S` language
- **milit ?**:`R+S` military
- **nerv 神经**:`R+S` nervous
- **numer 数**:`R+S` numerous
- **para ?**:`P+R` paragraph
- **phras ?**:`R+S` phrasal
- **pict ?**:`R+S` picture
- **plan ?**:`P+R+S` explanation
- **plas ?**:`R+R` plastic
- **plur ?**:`R+S` plural
- **propri 自己的**:`P+R+S` appropriate
- **radic 根**:`R+S` radical
- **reput ?**:`R+S` reputation
- **right ?**:`P+R` upright
- **soph ?**:`R+S+S` sophistication
- **spher 球**:`R+S` spherical
- **stud 学习**:`R+S` study
- **tal ?**:`R+S` talent
- **therm 热**:`R+R` thermometer
- **tot ?**:`R+S` total
- **turn ?**:`P+R` return
- **veget ?**:`R+S` vegetable

## 三、前缀生产力档案

| 前缀 | 词根结合产物 | 词基结合(P+词基) |
|---|---|---|
| **ad**(ad ac af ag al an ap ar as at) | 142:academic, accelerate, acceleration, accent, accept, acceptable, acceptance, access, accessible, accident, accidental, accommodate, accommodation, accompany… | — |
| **com**(com con col cor co cox) | 213:coherence, coherent, cohesion, cohesive, coinage, collaborate, collaboration, collaborative, collect, collection, collective, college, colorful, combat… | — |
| **re**(re) | 149:react, reaction, reactionary, rebellion, rebellious, receive, receiver, reception, receptionist, receptive, recession, recipient, recitation, recite… | recall, refresh, remark, remind, repay, retail, reunite, review |
| **in**(in im il ir ig i) | 182:ignorance, ignorant, ignore, illegal, illusion, illusory, image, immediate, immediately, immigrant, immigrate, immigration, impact, impatience… | income, indeed, indoor, indoors, inland, insight |
| **ex**(ex e ec ef es) | 116:early, edit, edition, editor, editorial, educate, education, educational, effect, effective, efficiency, efficient, effort, either… | — |
| **de**(de) | 86:deadly, debate, deceit, deceive, decent, deception, deceptive, decide, decision, decisive, declaration, declarative, declare, decline… | — |
| **sub**(sub suc suf sug sup sus) | 46:subject, subjective, submission, submissive, submit, subsequent, substance, substantial, substitute, substitution, subtract, subtraction, suburb, suburban… | subtitle, subway |
| **pre**(pre) | 39:preacher, precise, precisely, precision, predict, predictable, prediction, preface, prefer, preferable, preference, preferential, pregnancy, pregnant… | preview |
| **pro**(pro) | 52:proceed, process, procession, processor, proclaim, proclamation, produce, product, production, productive, productivity, profession, professional, professor… | — |
| **dis**(dis dif) | 43:differ, difference, different, difficulty, disability, disagree, disagreement, disappear, disappearance, disappoint, disappointing, disappointment, disapproval, disapprove… | — |
| **trans**(trans) | 15:transaction, transfer, transference, transform, transformation, translate, translation, translator, transmission, transmit, transmitter, transparency, transparent, transport… | — |
| **ob**(ob oc of op) | 39:obese, obesity, object, objection, objective, obligation, oblige, observant, observation, observe, observer, obtain, obtainment, obvious… | — |
| **inter**(inter intel enter) | 23:intellectual, intelligence, intelligent, interact, interaction, interactive, interest, interesting, interfere, interference, interior, intermediary, intermediate, international… | interview |
| **per**(per peri) | 27:perceive, percent, percentage, perception, perceptive, perfect, perfection, perform, performance, performer, permanence, permanent, permissible, permission… | — |
| **con**(con) | 0: | — |
| **un**(un) | 4:uncertainty, uncover, unemployment, unity | unavailable, uncover, undo, uneasy, unfair |
| **a**(a) | 0: | aboard, abroad, afraid, ahead, alike, alive, alone, ashamed, ashore, aside… |
| **e**(e) | 0: | — |
| **en**(en) | 17:encounter, encourage, encouragement, encouraging, endurance, endure, enforce, enforcement, enquire, enquiry, enroll, enrollment, ensure, entire… | — |
| **be**(be) | 11:beautiful, beautify, beauty, because, behave, behavior, behavioral, beside, besides, betray, betrayal | become, behalf, behind, beloved, below, beneath, beside, besides, between, beyond |
| **a**(a) | 0: | aboard, abroad, afraid, ahead, alike, alive, alone, ashamed, ashore, aside… |
| **e**(e) | 0: | — |
| **sur**(sur) | 7:surface, surprise, surrender, survey, survival, survive, survivor | surname, surplus |
| **super**(super supr) | 5:superficial, superior, supervise, supervision, supervisor | supermarket |
| **se**(se) | 17:search, secret, secretary, secretive, secure, security, seeming, seemingly, select, selection, selective, senate, separate, separation… | — |
| **di**(di) | 26:digest, digestion, digestive, diligence, diligent, dioxide, direct, direction, director, distance, distant, distinct, distinction, distinctive… | — |
| **dia**(dia) | 5:diagram, dialect, dialog, dialogue, diameter | — |
| **under**(under) | 5:underdeveloped, underestimate, undergraduate, understand, understanding | undergo, underground, underline, underneath, understand, underwear, unemployed |
| **over**(over) | 0: | overall, overcoat, overcome, overdue, overhead, overhear, overlook, overnight, overseas, oversee… |
| **out**(out) | 4:output, outside, outsider, outstanding | onset, outbreak, outcome, outdated, outdoor, outdoors, outlet, outline, outlook, outset… |
| **up**(up) | 2:upgrade, upright | up-to-date, update, upgrade, upload, upon, upper, upright, upset, upstairs |
| **down**(down) | 0: | download, downstairs, downtown, downward, downwards |
| **fore**(fore) | 0: | forecast, forehead, foremost, foresee, foreseeable |
| **by**(by) | 2:bypass, bystander | bypass, bystander |
| **with**(with) | 1:withstand | withdraw, withstand |
| **self**(self) | 0: | selfish, selfless |
| **semi**(semi) | 0: | semi-colon |
| **grand**(grand) | 0: | grandchild, granddaughter, grandfather, grandmother, grandparent, grandson |
| **homo**(homo) | 0: | homosexual |
| **micro**(micro) | 3:microphone, microscope, microscopic | microphone, microwavable, microwave |
| **tele**(tele) | 4:telephone, telescope, telescopic, television | telephone |
| **counter**(counter) | 1:counterpart | — |
| **anti**(anti) | 2:anticipate, anticipation | — |
| **ante**(ante anti) | 0: | — |
| **apo**(apo) | 4:apologetic, apologise, apologize, apology | — |
| **auto**(auto) | 5:automate, automatic, automatically, automation, automobile | — |
| **bene**(bene) | 3:beneficial, beneficiary, benefit | — |
| **circum**(circum) | 1:circumstance | — |
| **equi**(equi) | 2:equivalence, equivalent | — |
| **infra**(infra) | 1:infrastructure | — |
| **intro**(intro) | 3:introduce, introduction, introductory | — |
| **non**(non) | 1:nonsense | — |
| **mis**(mis) | 3:missile, misunderstand, misunderstanding | — |
| **para**(para) | 1:paragraph | — |
| **peri**(peri) | 0: | — |
| **post**(post) | 3:postman, postpone, postponement | — |
| **tri**(tri) | 1:triple | — |
| **uni**(uni) | 5:uniform, uniformity, universal, universe, university | — |
| **mal**(mal) | 0: | — |
| **mono**(mono) | 0: | — |
| **ultra**(ultra) | 0: | — |
| **amb**(amb) | 2:ambition, ambitious | — |
| **sym**(sym syn) | 4:sympathetic, sympathise, sympathize, sympathy | — |

## 四、后缀生产力档案

### 1. 词根型(R+S/…,引擎全列)

| 后缀 | 词数 | 产物(≤18 例) |
|---|---|---|
| -ion | 215 | absorption, action, addiction, addition, additional, administration, admission, adoption, affection, affectionate, aggression, alteration, ambition, assumption, attention, attraction, automation, battalion… |
| -al | 182 | accidental, actual, actually, additional, analytical, annual, approval, arrival, battalion, behavioral, beneficial, betrayal, capital, capitalism, capitalist, casual, central, centralise… |
| -ation | 124 | acceleration, accommodation, accumulation, accusation, adaptation, admiration, allegation, anticipation, application, approximation, association, authorisation, aviation, celebration, citation, clarification, classification, collaboration… |
| -ive | 81 | abusive, active, adaptive, addictive, adjective, administrative, aggressive, attentive, attractive, cohesive, collective, combative, comparative, competitive, comprehensive, conclusive, corrective, deceptive… |
| -er | 75 | administer, adviser, alter, banker, biographer, bystander, center, commander, commuter, composer, computer, computerise, computerize, conditioner, consider, considerable, considering, consumer… |
| -ment | 68 | achievement, adjustment, advancement, advertisement, agreement, announcement, apartment, appointment, assessment, assignment, astonishment, attachment, attainment, commitment, concealment, confinement, containment, department… |
| -ate | 67 | accommodate, accumulate, accurate, advocate, affectionate, anticipate, appropriate, approximate, approximately, associate, candidate, celebrate, certificate, collaborate, communicate, compensate, complicate, consolidate… |
| -or | 66 | actor, advisor, ancestor, author, authorise, authorization, authorize, behavior, behavioral, competitor, conductor, conqueror, contractor, coordinator, corridor, director, doctor, doctoral… |
| -ent | 61 | accident, accidental, affluent, agent, apparent, apparently, authentic, coherent, competent, component, confident, confidential, conscientious, consequent, consequently, consistent, continent, continental… |
| -ly | 52 | accordingly, actually, adequately, apparently, approximately, artistically, automatically, clearly, consequently, costly, currently, definitely, equally, especially, eventually, exceedingly, family, fatherly… |
| -y | 49 | accompany, apology, assembly, beauty, betray, betrayal, biography, colony, company, controversy, delivery, deputy, discovery, display, electricity, enquiry, fairy, geography… |
| -ence | 45 | affluence, audience, coherence, competence, conference, confidence, conscience, consequence, correspondence, difference, diligence, emergence, equivalence, essence, evidence, existence, impatience, incidence… |
| -ing | 42 | accordingly, accounting, acting, aging, banking, compelling, concerning, considering, convincing, corresponding, demanding, disappointing, during, encouraging, evening, exceedingly, exciting, founding… |
| -ity | 39 | ability, accountability, acidity, availability, clarity, commodity, community, complexity, conformity, continuity, dignity, disability, diversity, durability, extremity, generosity, gravity, humanity… |
| -ic | 39 | academic, atomic, authentic, automatic, classic, clinic, communicate, communication, communicative, cosmic, critic, criticise, criticism, criticize, democratic, diplomatic, dynamic, electric… |
| -able | 37 | acceptable, accountable, admirable, advisable, agreeable, applicable, available, comfortable, comparable, considerable, creditable, curable, disputable, durable, fashionable, honorable, hospitable, indispensable… |
| -ance | 35 | abundance, acceptance, accordance, acquaintance, admittance, alliance, ambulance, appearance, appliance, assistance, attendance, circumstance, compliance, conveyance, disappearance, distance, disturbance, dominance… |
| -ant | 32 | abundant, accountant, applicant, assistant, attendant, consultant, contestant, defendant, descendant, dominant, elegant, hesitant, ignorant, immigrant, important, informant, inhabitant, merchant… |
| -ical | 29 | artistically, automatically, biographical, biological, classical, clinical, critical, ecological, electrical, geographically, geological, geologically, geometrically, grammatical, graphically, identical, logical, medical… |
| -ty | 29 | authority, courtyard, equality, eventuality, facility, fatality, formality, honesty, hospitality, hostility, inferiority, liberty, locality, majority, mentality, minority, modesty, morality… |
| -us | 27 | advantageous, apparatus, campus, circus, conscious, consensus, continuous, courageous, disastrous, enormous, envious, famous, generous, genius, minus, monstrous, nervous, numerous… |
| -ise | 27 | advertise, advertisement, apologise, authorise, centralise, colonise, computerise, criticise, emphasise, finalise, generalise, legalise, memorise, neutralise, normalise, organise, penalise, practise… |
| -ize | 26 | apologize, authorize, centralize, colonize, computerize, criticize, emphasize, finalize, generalize, hospitalize, legalize, memorize, neutralize, normalize, organize, penalize, publicize, realize… |
| -ure | 24 | adventure, agriculture, architecture, capture, culture, departure, exposure, figure, future, gesture, infrastructure, lecture, leisure, leisurely, manufacture, mature, measure, measurement… |
| -ary | 23 | anniversary, commentary, contemporary, dictionary, evolutionary, extraordinary, fragmentary, intermediary, library, literary, military, ordinary, preliminary, primary, reactionary, revolutionary, salary, secretary… |
| -ative | 21 | accumulative, collaborative, communicative, conservative, consultative, cooperative, creative, declarative, demonstrative, derivative, illustrative, imitative, indicative, informative, innovative, narrative, preservative, provocative… |
| -ible | 18 | accessible, contemptible, convertible, feasible, flexible, horrible, impossible, incredible, intangible, invisible, permissible, possible, responsible, reversible, sensible, tangible, terrible, visible |
| -age | 18 | advantage, courage, coverage, discourage, encourage, encouragement, heritage, language, manage, management, orphanage, passage, percentage, stage, storage, usage, voltage, voyage |
| -ory | 17 | advisory, compensatory, compulsory, congratulatory, contradictory, discriminatory, dormitory, explanatory, factory, illusory, introductory, preparatory, regulatory, satisfactory, territory, theory, victory |
| -ify | 17 | beautify, clarify, classify, dignify, diversify, horrify, identify, intensify, justify, modify, notify, purify, qualify, quantify, solidify, specify, terrify |
| -ial | 13 | artificial, confidential, essential, influential, managerial, ministerial, potential, preferential, presidential, residential, sacrificial, sequential, territorial |
| -an | 12 | civilian, human, humanity, instantaneous, republican, spontaneous, suburban, urban, urbanisation, urbanise, urbanization, urbanize |
| -ious | 11 | ambitious, conscientious, contentious, curious, delicious, infectious, injurious, laborious, meritorious, religious, suspicious |
| -id | 11 | candidacy, candidate, consolidate, consolidation, fluid, invalid, rapid, solid, solidify, valid, validate |
| -ency | 10 | agency, consistency, currency, emergency, fluency, frequency, presidency, tendency, transparency, urgency |
| -ish | 10 | astonish, astonishment, distinguishable, finish, flourish, polish, publish, punish, punishment, vanish |
| -ist | 10 | biologist, capitalist, physicist, psychologist, receptionist, scientist, socialist, sociologist, terrorist, therapist |
| -ism | 10 | capitalism, communism, criticism, idealism, optimism, organism, realism, socialism, terrorism, tourism |
| -ry | 9 | ancestry, contrary, exemplary, ferry, honorary, marry, merry, ministry, rivalry |
| -ard | 9 | courtyard, regard, regarding, regardless, standard, standardisation, standardise, standardization, standardize |
| -ization | 8 | authorization, normalization, organization, organizational, realization, standardization, urbanization, utilization |
| -ful | 8 | colorful, distressful, eventful, powerful, respectful, stressful, successful, useful |
| -acy | 7 | accuracy, adequacy, advocacy, candidacy, literacy, privacy, supremacy |
| -istic | 7 | artistic, idealistic, optimistic, realistic, sophistication, statistic, statistical |
| -ient | 7 | convenient, deficient, efficient, nutrient, proficient, recipient, sufficient |
| -ice | 7 | justice, notice, noticeable, police, practice, prejudice, service |
| -isation | 7 | normalisation, organisation, organisational, realisation, standardisation, urbanisation, utilisation |
| -ular | 6 | circular, irregular, particular, particularly, regular, regularity |
| -tion | 6 | contribution, distribution, inspiration, notation, penetration, pollution |
| -ibility | 6 | feasibility, flexibility, impossibility, possibility, responsibility, visibility |
| -ess | 6 | heiress, hostess, possess, possession, possessive, priceless |
| -ous | 6 | humorous, marvelous, tremendous, victorious, vigorous, voluminous |
| -ancy | 5 | accountancy, expectancy, occupancy, pregnancy, vacancy |
| -ology | 5 | biotechnology, psychology, sociology, technology, zoology |
| -ulate | 5 | circulate, congratulate, formulate, regulate, speculate |
| -ulation | 5 | circulation, congratulation, formulation, regulation, speculation |
| -ational | 5 | educational, inspirational, occupational, recreational, vocational |
| -ition | 4 | acquisition, definition, nutrition, recognition |
| -ivity | 4 | activity, productivity, relativity, sensitivity |
| -ile | 4 | automobile, hostile, mobile, textile |
| -ship | 4 | championship, companionship, relationship, sponsorship |
| -ee | 4 | committee, employee, interviewee, trainee |
| -ite | 4 | definite, definitely, infinite, polite |
| -ician | 4 | electrician, physician, politician, technician |
| -fy | 4 | exemplify, satisfy, simplify, verify |
| -ine | 4 | famine, imagine, marine, medicine |
| -eful | 4 | forceful, grateful, peaceful, purposeful |
| -itive | 4 | infinitive, inquisitive, primitive, sensitive |
| -th | 4 | length, lengthy, month, monthly |
| -itude | 3 | altitude, aptitude, gratitude |
| -ey | 3 | attorney, honey, money |
| -um | 3 | auditorium, medium, volume |
| -logy | 3 | biology, ecology, geology |
| -iism | 3 | colonialism, imperialism, materialism |
| -iise | 3 | commercialise, materialise, specialise |
| -iize | 3 | commercialize, materialize, specialize |
| -iency | 3 | deficiency, efficiency, proficiency |
| -uable | 3 | distinguishable, invaluable, valuable |
| -ics | 3 | dynamics, physics, politics |
| -ety | 3 | entirety, society, variety |
| -uate | 3 | evaluate, graduate, undergraduate |
| -less | 3 | pointless, regardless, useless |
| -ery | 2 | battery, refinery |
| -iful | 2 | beautiful, merciful |
| -ainty | 2 | certainty, uncertainty |
| -ience | 2 | convenience, experience |
| -ulty | 2 | difficulty, faculty |
| -ide | 2 | dioxide, provide |
| -uation | 2 | evaluation, graduation |
| -iation | 2 | initiation, pronunciation |
| -eous | 2 | instantaneous, spontaneous |
| -ably | 2 | invariably, probably |
| -iist | 2 | materialist, specialist |
| -eing | 1 | ageing |
| -cy | 1 | bankruptcy |
| -ier | 1 | barrier |
| -iary | 1 | beneficiary |
| -sion | 1 | discussion |
| -uish | 1 | distinguish |
| -uary | 1 | documentary |
| -iture | 1 | expenditure |
| -uine | 1 | genuine |
| -oic | 1 | heroic |
| -oine | 1 | heroine |
| -oism | 1 | heroism |
| -uism | 1 | individualism |
| -enza | 1 | influenza |
| -iate | 1 | initiate |
| -iative | 1 | initiative |
| -ihood | 1 | livelihood |
| -hood | 1 | manhood |
| -ster | 1 | monster |
| -eness | 1 | politeness |
| -ily | 1 | primarily |
| -ision | 1 | provision |
| -ister | 1 | register |
| -ecy | 1 | secrecy |
| -iisation | 1 | specialisation |
| -iization | 1 | specialization |
| -ulative | 1 | speculative |
| -ual | 1 | spiritual |
| -uum | 1 | vacuum |
| -uous | 1 | virtuous |
| -uise | 1 | visualise |
| -uize | 1 | visualize |
| -eer | 1 | volunteer |

### 2. 词基型(D+S,与自由单词结合,计数+例)

| 后缀 | 义 | 词数 | 例 |
|---|---|---|---|
| -y | 多…的 | 148 | advisory, ally, any, apply, army, assembly |
| -ly | 如…的 | 109 | actually, adequately, allegedly, apparently, apply, approximately |
| -ful | 充满 | 53 | awful, beautiful, boastful, careful, cheerful, colorful |
| -en | 使、材料 | 34 | brighten, broaden, dampen, darken, drunken, even |
| -ry | 场所 | 26 | bakery, biochemistry, bravery, bribery, carry, chemistry |
| -ness | 状态 | 21 | awareness, business, consciousness, darkness, emptiness, goodness |
| -less | 无 | 19 | careless, doubtless, endless, flawless, harmless, helpless |
| -ery | 场所 | 16 | bakery, battery, bravery, bribery, every, forgery |
| -ship | 关系 | 15 | championship, citizenship, companionship, fellowship, friendship, hardship |
| -ish | 略…的 | 10 | childish, feverish, finish, flourish, foolish, polish |
| -ward | 向 | 9 | afterward, backward, downward, forward, inward, onward |
| -hood | 身份 | 7 | adulthood, falsehood, likelihood, livelihood, manhood, neighborhood |
| -dom | 领域 | 3 | freedom, kingdom, wisdom |
| -wise | 方式 | 3 | clockwise, likewise, otherwise |

## 五、本表内零产物成分(学习时可降级)

- **前缀**:ante, down, fore, homo, mal, mid, mono, over, self, semi, well(均无词根结合产物)
- **后缀**:esque, ian, ness, ward, wards, wise(词基型后缀已列于上表,不算零产物)
- **未入图谱的引擎余量**(2 词,多为词源不适用或音变不规则):remind, reminder