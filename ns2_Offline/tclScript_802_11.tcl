# ======================================================================
# Define options
set val(chan)           Channel/WirelessChannel  ;# channel type
set val(prop)           Propagation/TwoRayGround ;# radio-propagation model
set val(ant)            Antenna/OmniAntenna      ;# Antenna type
set val(ll)             LL                       ;# Link layer type
set val(ifq)            CMUPriQueue              ;# Interface queue type
set val(ifqlen)         50                       ;# max packet in ifq
set val(netif)          Phy/WirelessPhy          ;# network interface type
set val(mac)            Mac/802_11               ;# MAC type
set val(rp)             DSR                      ;# ad-hoc routing protocol 
set val(nn)             [lindex $argv 0]         ;# number of mobilenodes
set val(nf)             [lindex $argv 1]         ;# number of flows
set val(energymodel)    "EnergyModel"
set val(initialenergy)  5                        ;# Initial energy in Joules
set val(idlepower)      0.45                     ;#LEAP (802.11g)
set val(rxpower)        0.9                        ;#LEAP (802.11g)
set val(txpower)        0.9                        ;#LEAP (802.11g)
set val(sleeppower)     0.05                     ;#LEAP (802.11g)
set val(setX)           [lindex $argv 2]
set val(setY)           [lindex $argv 3]
# =======================================================================

# simulator
set ns [new Simulator]

# trace file
set trace_file [open trace.tr w]
$ns trace-all $trace_file
# $ns use-newtrace

# nam file
set nam_file [open animation.nam w]
$ns namtrace-all $nam_file
$ns namtrace-all-wireless $nam_file $val(setX) $val(setY)

# topology: to keep track of node movements
set topo [new Topography]
$topo load_flatgrid $val(setX) $val(setY)

# general operation director for mobilenodes
create-god $val(nn)

# node configs
# ======================================================================

# $ns node-config -addressingType flat or hierarchical or expanded
#                  -adhocRouting   DSDV or DSR or TORA
#                  -llType	   LL
#                  -macType	   Mac/802_11
#                  -propType	   "Propagation/TwoRayGround"
#                  -ifqType	   "Queue/DropTail/PriQueue"
#                  -ifqLen	   50
#                  -phyType	   "Phy/WirelessPhy"
#                  -antType	   "Antenna/OmniAntenna"
#                  -channelType    "Channel/WirelessChannel"
#                  -topoInstance   $topo
#                  -energyModel    "EnergyModel"
#                  -initialEnergy  (in Joules)
#                  -rxPower        (in W)
#                  -txPower        (in W)
#                  -agentTrace     ON or OFF
#                  -routerTrace    ON or OFF
#                  -macTrace       ON or OFF
#                  -movementTrace  ON or OFF

# ======================================================================

$ns node-config -addressingType flat \
                -adhocRouting $val(rp) \
                -llType $val(ll) \
                -macType $val(mac) \
                -ifqType $val(ifq) \
                -ifqLen $val(ifqlen) \
                -antType $val(ant) \
                -propInstance [new $val(prop)]\
                -phyType $val(netif) \
                -topoInstance $topo \
                -channel [new $val(chan)] \
                -agentTrace ON \
                -routerTrace ON \
                -macTrace ON \
                -movementTrace OFF \
                # -energyModel $val(energymodel) \
                # -initialEnergy $val(initialenergy) \
                # -rxPower $val(rxpower) \
                # -txPower $val(txpower)


# create nodes
set sqr [expr round(sqrt($val(nn))) + 1]
for {set i 0} {$i < $val(nn) } {incr i} {
    set node($i) [$ns node]
    $node($i) random-motion 0       ;# disable random motion

    $node($i) set X_ [expr ($i/$sqr) * ($val(setX)/$sqr) + 50]
    $node($i) set Y_ [expr ($i%$sqr) * ($val(setX)/$sqr) + 50]
    $node($i) set Z_ 0

    $ns initial_node_pos $node($i) 20

    set destX [expr (round(rand()*$val(setX))/2) + 50 ]
    set destY [expr (round(rand()*$val(setY))/2) + 50 ]
    set speed [expr round(rand()*4) + 1]

    $ns at 20.0 "$node($i) setdest $destX $destY $speed"
} 



# Traffic

# for {set i 0} {$i < $val(nf)} {incr i} {
#     set src $i
#     set dest [expr $i + 8]

#     # Traffic config
#     # create agent
#     set tcp [new Agent/TCP]
#     set tcp_sink [new Agent/TCPSink]
#     # attach to nodes
#     $ns attach-agent $node($src) $tcp
#     $ns attach-agent $node($dest) $tcp_sink
#     # connect agents
#     $ns connect $tcp $tcp_sink
#     $tcp set fid_ $i

#     # Traffic generator
#     set ftp [new Application/FTP]
#     # attach to agent
#     $ftp attach-agent $tcp
    
#     # start traffic generation
#     $ns at 1.0 "$ftp start"
# }


# Traffic 

for {set i 0} {$i < $val(nf)} {incr i} {
    set src [expr round(rand()*$val(nn))]
    set dest [expr round(rand()*$val(nn))]

    if {$src == $dest} {
        set dest [expr ($dest + 1)%$val(nn)]
    }

    if {$src  >= $val(nn)} {
        set src [expr ($src + 1)%$val(nn)]
    }

    if {$dest  >= $val(nn)} {
        set dest [expr ($dest + 1)%$val(nn)]
    }


    set tcp [new Agent/TCP/Reno]
    # $tcp set class_ 0
    # $tcp set window_ 50
    # $tcp set packetSize_ 50
    

    $ns attach-agent $node($src) $tcp

    # $tcp attach $trace_file
    # $tcp tracevar cwnd_
    # $tcp tracevar ssthresh_
    # $tcp tracevar ack_
    # $tcp tracevar maxseq_

    set end [new Agent/TCPSink]
    $ns attach-agent $node($dest) $end

    $ns connect $tcp $end

    set ftp [new Application/FTP]
    $ftp attach-agent $tcp
    $ns at 1 "$ftp start"
    $ns at 50 "$ftp stop"
}


# End Simulation

# Stop nodes
for {set i 0} {$i < 16} {incr i} {
    $ns at [expr 52] "$node($i) reset"
}

# call final function
proc finish {} {
    global ns trace_file nam_file
    $ns flush-trace
    close $trace_file
    close $nam_file
    exit 0
}

$ns at [expr 53] "finish"

# Run simulation
puts "Simulation starting"
$ns run
