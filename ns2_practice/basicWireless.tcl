# ====================================================================
# Define option
# ====================================================================

set val(chan)   Channel/WirelessChannel     ;# channel type
set val(prop)   Propagation/TwoRayGround    ;# radio-propagation model
set val(ant)    Antenna/OmniAntenna         ;# antenna type
set val(ll)     LL                          ;# link layer type
set val(ifq)    Queue/DropTail/PriQueue     ;# interface queue type
set val(ifqlen) 50                          ;# max packet in ifq
set val(netif)  Phy/WirelessPhy             ;# network interface type
set val(mac)    Mac/802_11                  ;# MAC type
set val(rp)     DSDV                        ;# ad-hoc routing protocol
set val(nn)     2                           ;# number of mobile nodes
set chan_(0) [new $val(chan)]
set chan_(1) [new $val(chan)]

# ====================================================================
# Setup
# ====================================================================

set ns_ [new Simulator]

set tf_ [open out.tr w]
$ns_ trace-all $tf_

set nf_ [open out.nam w]
$ns_ namtrace-all-wireless $nf_ 500 500

set topo [new Topography]
$topo load_flatgrid 500 500

create-god $val(nn)                         ;# needs to be created even if we don't need it

# ====================================================================
# node configuration
# ====================================================================

$ns_ node-config    -adhocRouting $val(rp) \
                    -llType $val(ll) \
                    -macType $val(mac) \
                    -ifqType $val(ifq) \
                    -ifqLen $val(ifqlen) \
                    -antType $val(ant) \
                    -propType $val(prop) \
                    -phyType $val(netif) \
                    -topoInstance $topo \
                    -agentTrace ON \
                    -routerTrace ON \
                    -macTrace OFF \
                    -movementTrace OFF

# ====================================================================
# node creation
# ====================================================================

for {set i 0} {$i < $val(nn)} {incr i} {
    $ns_ node-config -channel $chan_(0)
    set node_($i) [$ns_ node]
    $node_($i) random-motion 0
    $ns_ initial_node_pos $node_($i) 20
}

# ====================================================================
# node position
# ====================================================================

$node_(0) set X_ 5.0
$node_(0) set Y_ 2.0
$node_(0) set Z_ 0.0

$node_(1) set X_ 390.0
$node_(1) set Y_ 385.0
$node_(1) set Z_ 0.0

# at t node (i) starts to move at x, y with speed v
$ns_ at 50.0 "$node_(1) setdest 25.0 20.0 15.0"
$ns_ at 10.0 "$node_(0) setdest 20.0 18.0 1.0"

$ns_ at 100.0 "$node_(1) setdest 490.0 480.0 15.0"


set tcp [new Agent/TCP]
$tcp set class_ 2
set sink [new Agent/TCPSink]

$ns_ attach-agent $node_(0) $tcp
$ns_ attach-agent $node_(1) $sink
$ns_ connect $tcp $sink

set ftp [new Application/FTP]
$ftp attach-agent $tcp
$ns_ at 10.0 "$ftp start"

for {set i 0} {$i < $val(nn)} {incr i} {
    $ns_ at 150.0 "$node_($i) reset"
}

$ns_ at 150.0001 "stop"
$ns_ at 150.0002 "puts \"NS EXITING...\" ; $ns_ halt"
proc stop {} {
    global ns_ nf_ tf_
    $ns_ flush-trace
    close $nf_
    close $tf_
    exec nam out.nam &
    exit 0
}

puts "Starting Simulation..."
$ns_ run