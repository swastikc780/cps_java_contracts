package community.icon.cps.integration;

import com.eclipsesource.json.JsonObject;
import community.icon.cps.score.lib.interfaces.CPSCoreInterface;
import community.icon.cps.score.lib.interfaces.CPSCoreInterface.MilestonesAttributes;
import community.icon.cps.score.lib.interfaces.CPSCoreInterface.MilestoneVoteAttributes;
import community.icon.cps.score.lib.interfaces.CPSCoreInterface.ProposalAttributes;
import community.icon.cps.score.lib.interfaces.CPSCoreInterface.ProgressReportAttributes;
import community.icon.cps.score.test.integration.scores.CPSCoreInterfaceScoreClient;
import community.icon.cps.score.test.integration.CPS;
import community.icon.cps.score.test.integration.CPSClient;
import community.icon.cps.score.test.integration.ScoreIntegrationTest;
import community.icon.cps.score.test.integration.config.BaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import score.Address;

import static community.icon.cps.score.cpscore.utils.Constants.*;
import static community.icon.cps.score.test.AssertRevertedException.assertUserRevert;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static community.icon.cps.score.test.integration.Environment.preps;

import community.icon.cps.score.test.integration.scores.SystemInterfaceScoreClient;
import score.UserRevertException;
import score.annotation.Optional;

public class CPSCoreIntegration implements ScoreIntegrationTest {

    private static CPSClient ownerClient;
    private static CPSClient testClient;
    private static CPSClient readerClient;
    static Set<Map.Entry<Address, String>> prepSet = preps.entrySet();

    private final BigInteger ICX = BigInteger.valueOf(10).pow(18);

    private static Map<String, foundation.icon.jsonrpc.Address> addressMap;
    public static Map<Integer, CPSClient> cpsClients = new HashMap<>();

    @BeforeAll
    public static void setup() throws Exception {
        String contracts = "conf/contracts.json";
        CPS cps = new CPS(contracts);

        cps.setupCPS();
        addressMap = cps.getAddresses();
        ownerClient = cps.defaultClient();
        readerClient = cps.newClient(BigInteger.TEN.pow(24));
        testClient = cps.testClient();
        BaseConfig config = new BaseConfig(ownerClient);
        config.call();

        for (int i = 0; i < 7; i++) {
            String privKey = prepSet.toArray()[i].toString().substring(43);
            cpsClients.put(i, cps.customClient(privKey));

        }

    }

    @Test
    @Order(1)
    public void name() {
        ownerClient.cpsCore.name();
    }


    @DisplayName("register all preps")
    @Test
    @Order(1)
    public void registerPrep() {

        // expected Prep address
        List<String> expectedPrepAddress = new ArrayList<>(cpsClients.size());
        for (CPSClient prepClient : cpsClients.values()) {
            prepClient.cpsCore.registerPrep();
            expectedPrepAddress.add(prepClient.getAddress().toString());

        }

        // actual prep address
        List<Map<String, Object>> preps = ownerClient.cpsCore.getPReps();
        List<String> actualPrepAddress = new ArrayList<>(preps.size());
        for (Map<String, Object> prep : preps) {
            actualPrepAddress.add((String) prep.get("address"));
        }

        assertEquals(expectedPrepAddress, actualPrepAddress);
//        SYSTEM_INTERFACE = new SystemInterfaceScoreClient(godClient);
//        System.out.println(SYSTEM_INTERFACE.getPRepTerm());
    }

    @Test
    @Order(2)
    public void registerPrepExceptions() {

        // readerClient tries to be registered as prep
        assertUserRevert(new UserRevertException(TAG + ": Not a P-Rep"),
                () -> readerClient.cpsCore.registerPrep(), null);

        // registered prep registers again
        CPSClient registeredPrep = cpsClients.get(0);

        assertUserRevert(new UserRevertException(TAG + ": P-Rep is already registered."),
                () -> registeredPrep.cpsCore.registerPrep(), null);


    }

    @DisplayName("unregister prep")
    @Test
    @Order(2)
    public void unregisterPrep() {
        CPSClient prep1 = cpsClients.get(0);
        prep1.cpsCore.unregisterPrep();

        // expected unregistered prep status
        Map<String, BigInteger> expectedPrepData = unregisteredPrepData();

        Map<String, BigInteger> actualPrepData = loginPrep(prep1.getAddress());
        assertEquals(expectedPrepData, actualPrepData);

        prep1.cpsCore.registerPrep();
    }


    @DisplayName("check prep data")
    @Test
    @Order(3)
    public void loginAsPrep() {
        // query period status
        Map<String, ?> periodStatus = getPeriodStatus();
        assertEquals(periodStatus.get(PERIOD_NAME), "Application Period");

        CPSClient prep1 = cpsClients.get(0);
        // expected prep1 status
        Map<String, BigInteger> expectedPrepData = votingPrepData();

        Map<String, BigInteger> actualPrepData = loginPrep(prep1.getAddress());
        assertEquals(expectedPrepData, actualPrepData);
    }

    @DisplayName("submit proposal 1")
    @Test
    @Order(4)
    public void submitProposal() {
        CPSClient prep1 = cpsClients.get(0);
        ProposalAttributes proposalAttributes = new ProposalAttributes();

        proposalAttributes.ipfs_hash = "Test_Proposal_1";
        proposalAttributes.project_title = "Proposal_1";
        proposalAttributes.project_duration = 3;
        proposalAttributes.total_budget = BigInteger.valueOf(100);
        proposalAttributes.token = bnUSD;
        proposalAttributes.sponsor_address = prep1.getAddress();
        proposalAttributes.ipfs_link = "https://proposal_1";
        proposalAttributes.milestoneCount = 3;
        proposalAttributes.isMilestone = true;

        CPSCoreInterface.MilestonesAttributes milestonesAttributes1 = new CPSCoreInterface.MilestonesAttributes();
        milestonesAttributes1.completionPeriod = 1;
        milestonesAttributes1.budget = BigInteger.valueOf(30).multiply(ICX);
        milestonesAttributes1.id = 1;

        CPSCoreInterface.MilestonesAttributes milestonesAttributes2 = new CPSCoreInterface.MilestonesAttributes();
        milestonesAttributes2.completionPeriod = 2;
        milestonesAttributes2.budget = BigInteger.valueOf(40).multiply(ICX);
        milestonesAttributes2.id = 2;

        CPSCoreInterface.MilestonesAttributes milestonesAttributes3 = new CPSCoreInterface.MilestonesAttributes();
        milestonesAttributes3.completionPeriod = 3;
        milestonesAttributes3.budget = BigInteger.valueOf(20).multiply(ICX);
        milestonesAttributes3.id = 3; // TODO: check if same id is sent

        CPSCoreInterface.MilestonesAttributes[] milestonesAttributes = new CPSCoreInterface.MilestonesAttributes[]
                {milestonesAttributes1, milestonesAttributes2, milestonesAttributes3};
        ((CPSCoreInterfaceScoreClient) testClient.cpsCore).submitProposal(BigInteger.valueOf(50).multiply(ICX), proposalAttributes, milestonesAttributes);

        List<String> proposalsIpfs = readerClient.cpsCore.getProposalsKeysByStatus(SPONSOR_PENDING);
        assertEquals(proposalsIpfs.size(), 1);
        assertEquals(proposalsIpfs.get(0), proposalAttributes.ipfs_hash);

        verifyProposalDetails(proposalAttributes);
    }

    private void verifyProposalDetails(ProposalAttributes expectedDetails) {
        Map<String, Object> actualDetails = getProposalDetails(expectedDetails.ipfs_hash);
        assertEquals(actualDetails.get("project_title"), expectedDetails.project_title);
        assertEquals(actualDetails.get("sponsor_address"), expectedDetails.sponsor_address.toString());
        assertEquals(actualDetails.get("ipfs_hash"), expectedDetails.ipfs_hash);
        assertEquals(toInt((String) actualDetails.get("project_duration")), expectedDetails.project_duration);
        assertEquals(toInt((String) actualDetails.get("milestoneCount")), expectedDetails.milestoneCount);
        assertEquals(toBigInt((String) actualDetails.get("total_budget")), expectedDetails.total_budget.multiply(ICX));

    }


    @DisplayName("submit sponsor vote on first proposal")
    @Test
    @Order(5)
    public void submitSponsorVote() {
        CPSClient sponsorPrep = cpsClients.get(0);

        bnUSDMint(sponsorPrep.getAddress(), BigInteger.valueOf(50).multiply(ICX));

        byte[] data = createByteArray("sponsorVote", "Test_Proposal_1", ACCEPT,
                "Proposal looks good");
        BigInteger sponsorBond = BigInteger.valueOf(15).multiply(ICX);
        sponsorPrep.bnUSD.transfer(addressMap.get("cpsCore"), sponsorBond, data);

        List<String> proposalsIpfs = readerClient.cpsCore.getProposalsKeysByStatus(PENDING);
        assertEquals(proposalsIpfs.size(), 1);
        assertEquals(proposalsIpfs.get(0), "Test_Proposal_1");


        Map<String, Object> proposalDetails = getProposalDetails("Test_Proposal_1");
        assertEquals(sponsorBond, toBigInt((String) proposalDetails.get(SPONSOR_DEPOSIT_AMOUNT)));
        assertEquals(BOND_RECEIVED, proposalDetails.get(SPONSOR_DEPOSIT_STATUS));

        assertEquals(BigInteger.valueOf(35).multiply(ICX), readerClient.bnUSD.balanceOf(sponsorPrep.getAddress()));
    }

    @DisplayName("vote on first proposal")
    @Test()
    @Order(6)
    public void voteProposal() throws InterruptedException {
        // update Period
        updateNextBlock();
        ownerClient.cpsCore.updatePeriod();

        Map<String, ?> periodStatus = getPeriodStatus();
        assertEquals(periodStatus.get(PERIOD_NAME), "Voting Period");

        Map<String, Object> proposalVote = getProposalVote("Test_Proposal_1");
        assertEquals(toInt((String) proposalVote.get("total_voters")), 0);
        assertEquals(toInt((String) proposalVote.get("approve_voters")), 0);
        assertEquals(toInt((String) proposalVote.get("reject_voters")), 0);

        for (CPSClient prepClient : cpsClients.values()) {
            voteByPrep(prepClient, "Test_Proposal_1", APPROVE, "Seems fruitful", false);
            prepClient.cpsCore.votePriority(new String[]{"Test_Proposal_1"});

        }
        proposalVote = getProposalVote("Test_Proposal_1");
        assertEquals(toInt((String) proposalVote.get("total_voters")), 7);
        assertEquals(toInt((String) proposalVote.get("approve_voters")), 7);
        assertEquals(toInt((String) proposalVote.get("reject_voters")), 0);
        // TODO: approved votes does not match up
//        assertEquals(toBigInt((String)proposalVote.get("approved_votes")),BigInteger.valueOf(9540000).multiply(ICX));
    }

    @DisplayName("pass proposal 1 and send the initial payement")
    @Test
    @Order(7)
    public void transferFundsToProposal() throws InterruptedException {
        updateToApplicationPeriod();
        Map<String, ?> periodStatus = getPeriodStatus();
        assertEquals(periodStatus.get(PERIOD_NAME), "Application Period");

        Map<String, ?> contributorProject = getContributorFunds(testClient.getAddress());
        List<Map<String, ?>> contributorData = (List<Map<String, ?>>) contributorProject.get("data");

        assertEquals(toBigInt((String) contributorData.get(0).get("installment_amount")), BigInteger.valueOf(30).multiply(ICX));
        assertEquals(toBigInt((String) contributorData.get(0).get("total_budget")), BigInteger.valueOf(100).multiply(ICX));
        assertEquals(contributorData.get(0).get("total_installment_count"), "0x3");
        assertEquals(toBigInt((String) contributorData.get(0).get("total_installment_paid")), BigInteger.valueOf(10).multiply(ICX));
        assertEquals(contributorProject.get("project_count"), "0x1");


        Map<String, ?> sponsorProject = getSponsorFunds(cpsClients.get(0).getAddress());

        List<Map<String, ?>> sponsorData = (List<Map<String, ?>>) sponsorProject.get("data");
        assertEquals(toBigInt((String) sponsorData.get(0).get("installment_amount")), BigInteger.valueOf(6).multiply(ICX).divide(BigInteger.TEN));
        assertEquals(toBigInt((String) sponsorData.get(0).get("sponsor_bond_amount")), BigInteger.valueOf(15).multiply(ICX));
        assertEquals(sponsorData.get(0).get("total_installment_count"), "0x3");
        assertEquals(toBigInt((String) sponsorData.get(0).get("total_installment_paid")), BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.TEN));
        assertEquals(toBigInt((String) sponsorProject.get("withdraw_amount_bnUSD")), BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.TEN));
    }


    private Map<String, ?> getContributorFunds(Address contributorAddr) {
        return readerClient.cpsTreasury.getContributorProjectedFund(contributorAddr);
    }

    private Map<String, ?> getSponsorFunds(Address sponsorAddr) {
        return readerClient.cpsTreasury.getSponsorProjectedFund(sponsorAddr);
    }

    @DisplayName("submit first milestone")
    @Test
    @Order(8)
    public void submitMilestoneReport() {

        List<String> proposalsIpfs = readerClient.cpsCore.getProposalsKeysByStatus(ACTIVE);
        assertEquals(proposalsIpfs.size(), 1);
        assertEquals(proposalsIpfs.get(0), "Test_Proposal_1");

        List<Map<String, Object>> activeProposal = readerClient.cpsCore.getActiveProposals(testClient.getAddress());
        assertEquals("0x0", activeProposal.get(0).get("new_progress_report"));

        CPSCoreInterface.ProgressReportAttributes progressReportAttributes = createProgressReport("1", "Test_Proposal_1");

        CPSCoreInterface.MilestoneSubmission submission = new CPSCoreInterface.MilestoneSubmission();
        submission.id = 1;
        submission.status = true;
        CPSCoreInterface.MilestoneSubmission[] milestoneSubmissions = new CPSCoreInterface.MilestoneSubmission[]{submission};

        testClient.cpsCore.submitProgressReport(progressReportAttributes, milestoneSubmissions);

        activeProposal = readerClient.cpsCore.getActiveProposals(testClient.getAddress());
        assertEquals("0x1", activeProposal.get(0).get("new_progress_report"));

        Map<String, Object> progressReportDetails = getProgressReportDetails("Test_Proposal_1_Report_Proposal_1");
        assertEquals(1, toInt((String) progressReportDetails.get(MILESTONE_SUBMITTED_COUNT)));
        assertEquals(WAITING, progressReportDetails.get(STATUS));
        assertEquals("Test_Proposal_1_Report_1", progressReportDetails.get(PROGRESS_REPORT_TITLE));
        assertEquals("Test_Proposal_1_Report_Proposal_1", progressReportDetails.get(REPORT_HASH));

        int milestoneStatus = getMilestoneStatus("Test_Proposal_1", 1);
        assertEquals(MILESTONE_REPORT_COMPLETED, milestoneStatus);
    }

    private ProgressReportAttributes createProgressReport(String no, String ipfsHash) {
        ProgressReportAttributes progressReportAttributes = new ProgressReportAttributes();
        progressReportAttributes.ipfs_hash = ipfsHash;
        progressReportAttributes.progress_report_title = ipfsHash + "_Report_" + no;
        progressReportAttributes.report_hash = ipfsHash + "_Report_Proposal_" + no;
        progressReportAttributes.ipfs_link = "https://proposal_" + no;
        progressReportAttributes.budget_adjustment = false;
        progressReportAttributes.additional_budget = BigInteger.ZERO;
        progressReportAttributes.additional_month = 0;

        return progressReportAttributes;

    }


    @DisplayName("vote on first progress report")
    @Test
    @Order(9)
    public void voteOnMilestone() throws InterruptedException {
        updateNextBlock();
        ownerClient.cpsCore.updatePeriod();

        Thread.sleep(2000);
        Map<String, ?> periodStatus = getPeriodStatus();
        assertEquals(periodStatus.get(PERIOD_NAME), "Voting Period");

        MilestoneVoteAttributes[] voteAttributes = new MilestoneVoteAttributes[]{vote(1, APPROVE)};

        List<Map<String, Object>> expectedMilestoneData = new ArrayList<>(7);
        for (int i = 0; i < cpsClients.size(); i++) {
            cpsClients.get(i).cpsCore.voteProgressReport("Test_Proposal_1_Report_Proposal_1", "Working well " + i,
                    voteAttributes, null, false);
            expectedMilestoneData.add(Map.of(
                    ADDRESS, cpsClients.get(i).getAddress(),
                    PREP_NAME, cpsClients.get(i).getAddress(),
                    VOTE_REASON, "Working well " + i,
                    VOTE, APPROVE));
        }

        Map<String, Object> progressReportVote = getProgressReportVote("Test_Proposal_1_Report_Proposal_1");
        assertEquals(toInt((String) progressReportVote.get("total_voters")), 7);

        Map<String, Object> milestoneVoteResult = getMilestoneReport("Test_Proposal_1_Report_Proposal_1", 1);
        assertEquals(toInt((String) milestoneVoteResult.get(APPROVE_VOTERS)), 7);
        assertEquals(toInt((String) milestoneVoteResult.get(REJECT_VOTERS)), 0);

        List<Map<String, Object>> actualMilestoneData = (List<Map<String, Object>>) milestoneVoteResult.get(DATA);


        for (int i = 0; i < 7; i++) {
            assertEquals(expectedMilestoneData.get(0).get(ADDRESS).toString(), actualMilestoneData.get(0).get(ADDRESS));
        }

        int milestoneStatus = getMilestoneStatus("Test_Proposal_1", 1);
        assertEquals(MILESTONE_REPORT_COMPLETED, milestoneStatus);


    }

    private int getMilestoneStatus(String ipfsHash, int milestoneId) {
        return readerClient.cpsCore.getMileststoneStatusOf(ipfsHash, milestoneId);
    }

    private Map<String, Object> getMilestoneReport(String reportHash, int milestoneId) {
        return readerClient.cpsCore.getMilestoneVoteResult(reportHash, milestoneId);
    }

    private Map<String, Object> getProgressReportDetails(String reportHash) {
        return readerClient.cpsCore.getProgressReportsByHash(reportHash);
    }

    private Map<String, Object> getProgressReportVote(String reportHash) {
        return readerClient.cpsCore.getProgressReportResult(reportHash);
    }

    @DisplayName("submitting all remaining milestones")
    @Test
    @Order(10)
    public void submitAllMilestoneReport() throws InterruptedException {
        updateToApplicationPeriod();
        Map<String, ?> periodStatus = getPeriodStatus();
        assertEquals(periodStatus.get(PERIOD_NAME), "Application Period");

        int milestoneStatus = getMilestoneStatus("Test_Proposal_1", 1);
        assertEquals(MILESTONE_REPORT_APPROVED, milestoneStatus);


        // verifying installment count and funds record
        Map<String, ?> contributorProject = getContributorFunds(testClient.getAddress());
        List<Map<String, ?>> contributorData = (List<Map<String, ?>>) contributorProject.get("data");
        assertEquals(toBigInt((String) contributorData.get(0).get("installment_amount")), BigInteger.valueOf(40).multiply(ICX));
        assertEquals(toBigInt((String) contributorData.get(0).get("total_installment_paid")), BigInteger.valueOf(40).multiply(ICX));
        assertEquals(contributorData.get(0).get("total_times_installment_paid"), "0x1");
        assertEquals(contributorProject.get("project_count"), "0x1");
        assertEquals(toBigInt((String) contributorProject.get("withdraw_amount_bnUSD")), BigInteger.valueOf(40).multiply(ICX));


        Map<String, ?> sponsorProject = getSponsorFunds(cpsClients.get(0).getAddress());
        List<Map<String, ?>> sponsorData = (List<Map<String, ?>>) sponsorProject.get("data");
        assertEquals(toBigInt((String) sponsorData.get(0).get("installment_amount")), BigInteger.valueOf(6).multiply(ICX).divide(BigInteger.TEN));
        assertEquals(toBigInt((String) sponsorData.get(0).get("total_installment_paid")), BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN));
        assertEquals(sponsorData.get(0).get("total_times_installment_paid"), "0x1");
        assertEquals(toBigInt((String) sponsorProject.get("withdraw_amount_bnUSD")), BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN));


        CPSCoreInterface.ProgressReportAttributes progressReportAttributes = createProgressReport("2", "Test_Proposal_1");

        CPSCoreInterface.MilestoneSubmission submissionNo2 = new CPSCoreInterface.MilestoneSubmission();
        submissionNo2.id = 2;
        submissionNo2.status = true;

        CPSCoreInterface.MilestoneSubmission submissionNo3 = new CPSCoreInterface.MilestoneSubmission();
        submissionNo3.id = 3;
        submissionNo3.status = true;

        CPSCoreInterface.MilestoneSubmission[] milestoneSubmissions = new CPSCoreInterface.MilestoneSubmission[]{
                submissionNo2, submissionNo3};

        testClient.cpsCore.submitProgressReport(progressReportAttributes, milestoneSubmissions);


        Map<String, Object> progressReportDetails = getProgressReportDetails("Test_Proposal_1_Report_Proposal_2");
        assertEquals(2, toInt((String) progressReportDetails.get(MILESTONE_SUBMITTED_COUNT)));
//        assertEquals(new String[]{"0x2","0x3"},progressReportDetails.get("milestoneId"));
        assertEquals(WAITING, progressReportDetails.get(STATUS));
        assertEquals("Test_Proposal_1_Report_2", progressReportDetails.get(PROGRESS_REPORT_TITLE));
        assertEquals("Test_Proposal_1_Report_Proposal_2", progressReportDetails.get(REPORT_HASH));

    }


    @DisplayName("vote on progress report 2")
    @Test
    @Order(11)
    public void voteOnAllMilestone() throws InterruptedException {

        updateNextBlock();
        ownerClient.cpsCore.updatePeriod();

        Thread.sleep(2000);
        Map<String, ?> periodStatus = getPeriodStatus();
        assertEquals(periodStatus.get(PERIOD_NAME), "Voting Period");

        MilestoneVoteAttributes[] voteAttributes = new MilestoneVoteAttributes[]{vote(2, APPROVE), vote(3, APPROVE)};

        List<Map<String, Object>> expectedMilestoneData = new ArrayList<>(7);
        for (int i = 0; i < cpsClients.size(); i++) {
            cpsClients.get(i).cpsCore.voteProgressReport("Test_Proposal_1_Report_Proposal_2", "Project is completed " + i,
                    voteAttributes, null, false);
            expectedMilestoneData.add(Map.of(
                    ADDRESS, cpsClients.get(i).getAddress(),
                    PREP_NAME, cpsClients.get(i).getAddress(),
                    VOTE_REASON, "Project is completed " + i,
                    VOTE, APPROVE));
        }

        Map<String, Object> progressReportVote = getProgressReportVote("Test_Proposal_1_Report_Proposal_2");
        assertEquals(toInt((String) progressReportVote.get("total_voters")), 7);

        Map<String, Object> milestoneVoteResult = getMilestoneReport("Test_Proposal_1_Report_Proposal_2", 2);
        assertEquals(toInt((String) milestoneVoteResult.get(APPROVE_VOTERS)), 7);
        assertEquals(toInt((String) milestoneVoteResult.get(REJECT_VOTERS)), 0);

//        assertEquals(milestoneVoteResult.get(DATA),expectedMilestoneData);
        List<Map<String, Object>> actualMilestoneData = (List<Map<String, Object>>) milestoneVoteResult.get(DATA);


        for (int i = 0; i < 7; i++) {
            assertEquals(expectedMilestoneData.get(0).get(ADDRESS).toString(), actualMilestoneData.get(0).get(ADDRESS));
            assertEquals(expectedMilestoneData.get(0).get(PREP_NAME).toString(), actualMilestoneData.get(0).get(PREP_NAME));
            assertEquals(expectedMilestoneData.get(0).get(VOTE_REASON), actualMilestoneData.get(0).get(VOTE_REASON));
            assertEquals(expectedMilestoneData.get(0).get(VOTE), actualMilestoneData.get(0).get(VOTE));

        }

        int milestoneStatus = getMilestoneStatus("Test_Proposal_1", 2);
        assertEquals(MILESTONE_REPORT_COMPLETED, milestoneStatus);

    }

    @DisplayName("proposal 1 is completed and user claims reward")
    @Test
    @Order(12)
    public void completeProposal1() throws InterruptedException {
        updateToApplicationPeriod();

        int milestoneStatus = getMilestoneStatus("Test_Proposal_1", 3);
        assertEquals(MILESTONE_REPORT_APPROVED, milestoneStatus);

        milestoneStatus = getMilestoneStatus("Test_Proposal_1", 2);
        assertEquals(MILESTONE_REPORT_APPROVED, milestoneStatus);

        List<String> proposalsIpfs = readerClient.cpsCore.getProposalsKeysByStatus(COMPLETED);
        assertEquals(proposalsIpfs.size(), 1);
        assertEquals(proposalsIpfs.get(0), "Test_Proposal_1");


        assertEquals(BigInteger.ZERO, readerClient.bnUSD.balanceOf(testClient.getAddress()));
        testClient.cpsTreasury.claimReward();
        assertEquals(BigInteger.valueOf(100).multiply(ICX), readerClient.bnUSD.balanceOf(testClient.getAddress()));

        CPSClient sponsorClient = cpsClients.get(0);
        assertEquals(BigInteger.valueOf(35).multiply(ICX), readerClient.bnUSD.balanceOf(sponsorClient.getAddress()));
        sponsorClient.cpsTreasury.claimReward();
        assertEquals(BigInteger.valueOf(37).multiply(ICX), readerClient.bnUSD.balanceOf(sponsorClient.getAddress()));


    }




    private MilestoneVoteAttributes vote(int id, String vote) {

        MilestoneVoteAttributes milestoneVoteAttributes = new MilestoneVoteAttributes();
        milestoneVoteAttributes.id = id;
        milestoneVoteAttributes.vote = vote;
        return milestoneVoteAttributes;
    }


    private void updateToApplicationPeriod() throws InterruptedException {
        updateNextBlock();
        ownerClient.cpsCore.updatePeriod();
        ownerClient.cpsCore.updatePeriod();
        ownerClient.cpsCore.updatePeriod();
        ownerClient.cpsCore.updatePeriod();
    }


    private BigInteger toBigInt(String inputString) {
        return new BigInteger(inputString.substring(2), 16);
    }

    private Integer toInt(String inputString) {
        return Integer.parseInt(inputString.substring(2));
    }


    private void voteByPrep(CPSClient caller, String ipfsKey, String vote, String voteReason,
                            @Optional boolean voteChange) {

        caller.cpsCore.voteProposal(ipfsKey, vote, voteReason, voteChange);
    }

    private Map<String, Object> getProposalDetails(String ipfsHash) {
        return readerClient.cpsCore.getProposalDetailsByHash(ipfsHash);
    }

    private Map<String, Object> getProposalVote(String ipfsHash) {
        return readerClient.cpsCore.getVoteResult(ipfsHash);
    }

    private void updateNextBlock() throws InterruptedException {
        ownerClient.cpsCore.updateNextBlock(1);
        Thread.sleep(2000);
    }

    private byte[] createByteArray(String methodName, String ipfsHash, String vote, String voteReason) {

        JsonObject internalParameters = new JsonObject()
                .add("ipfs_hash", String.valueOf(ipfsHash))
                .add("vote", String.valueOf(vote))
                .add("vote_reason", String.valueOf(voteReason));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }

    private void bnUSDMint(Address to, BigInteger amount){
        ownerClient.bnUSD.mintTo(to,amount);
    }


    private Map<String, ?> getPeriodStatus() {
        return readerClient.cpsCore.getPeriodStatus();
    }

    private Map<String, BigInteger> loginPrep(Address prepAddress) {
        return readerClient.cpsCore.loginPrep(prepAddress);
    }

    private Map<String, BigInteger> unregisteredPrepData() {
        return Map.of(IS_PREP, BigInteger.ONE,
                IS_REGISTERED, BigInteger.ZERO,
                PAY_PENALTY, BigInteger.ZERO,
                VOTING_PREP, BigInteger.ZERO);
    }

    private Map<String, BigInteger> votingPrepData() {
        return Map.of(IS_PREP, BigInteger.ONE,
                IS_REGISTERED, BigInteger.ONE,
                PAY_PENALTY, BigInteger.ZERO,
                VOTING_PREP, BigInteger.ONE);
    }

    private Map<String, BigInteger> registerPrepData() {
        return Map.of(IS_PREP, BigInteger.ONE,
                IS_REGISTERED, BigInteger.ONE,
                PAY_PENALTY, BigInteger.ZERO,
                VOTING_PREP, BigInteger.ZERO);
    }

    private Map<String, BigInteger> denyPrepData() {
        return Map.of(IS_PREP, BigInteger.ONE,
                IS_REGISTERED, BigInteger.ZERO,
                PAY_PENALTY, BigInteger.ONE,
                PENALTY_AMOUNT1, (BigInteger.TWO.multiply(ICX)).divide(BigInteger.valueOf(100)),
                VOTING_PREP, BigInteger.ZERO);
    }


}
